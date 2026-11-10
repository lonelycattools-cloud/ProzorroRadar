package ua.gov.prozorro.radar.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.data.repository.TenderRepository
import ua.gov.prozorro.radar.model.FeedItem
import ua.gov.prozorro.radar.model.ScanProgress
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.service.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScanController {
    @Volatile
    var isCancellationRequested: Boolean = false

    fun requestStop() {
        isCancellationRequested = true
    }

    fun reset() {
        isCancellationRequested = false
    }
}

class ProzorroScannerEngine(private val context: Context) {

    companion object {
        const val BASE_API_URL = "https://public.api.openprocurement.org/api/2.5/tenders"
        private val scanMutex = Mutex()
        @Volatile
        var isScanRunning: Boolean = false
            private set
    }

    private val apiService = ProzorroApiService()
    private val repository = TenderRepository.getInstance(context)
    private val preferences = AppPreferences(context)
    private val notificationHelper = NotificationHelper(context)
    private val emailSender = EmailSender(context)

    fun requestStop() {
        ScanController.requestStop()
    }

    suspend fun scanNow(
        startDateIso: String? = null,
        deepScan: Boolean = false,
        resetCheckpoint: Boolean = false
    ): ScanResult = withContext(Dispatchers.IO) {
        if (!scanMutex.tryLock()) {
            return@withContext ScanResult(
                success = false,
                newCount = 0,
                examinedCount = 0,
                message = "Сканування вже виконується в іншому потоці"
            )
        }

        var totalExamined = 0
        try {
            isScanRunning = true
            ScanController.reset()
            repository.setScanning(true)
            repository.updateScanProgress(ScanProgress(0, 0, "", "Ініціалізація пошуку у стрічці Prozorro…"))
            val startTime = System.currentTimeMillis()
            var totalFound = 0

            if (resetCheckpoint) {
                preferences.resetSyncOffset()
            }

            val cpvPrefixes = preferences.getCleanCpvPrefixes()
            val searchStatus = preferences.searchStatus.trim()

            // Existing IDs in SQLite for the CURRENT CPV categories
            val existingIds = repository.getExistingIds(cpvPrefixes).toMutableSet()

            val isCustomStart = !startDateIso.isNullOrBlank()
            // History/Deep mode: if explicitly requested, custom date, checkpoint reset, or if fewer than 50 tenders exist
            val isDeepHistoryMode = deepScan || isCustomStart || resetCheckpoint || existingIds.size < 50

            val thirtyDaysAgoIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
            )

            // Stop threshold on duplicates: ONLY in pure incremental monitor mode when DB already has ample tenders
            val currentStopThreshold = if (!isDeepHistoryMode && existingIds.isNotEmpty()) {
                minOf(preferences.stopOnDuplicates, existingIds.size)
            } else {
                999999
            }

            // Prozorro descending feed URL
            var url = "${ProzorroApiService.BASE_API_URL}?descending=1&limit=50&opt_fields=id,status,dateModified"

            val modeName = when {
                isCustomStart -> "З дати: ${startDateIso?.take(10)}"
                deepScan -> "ГЛИБОКИЙ ПОШУК (30 днів, База: ${existingIds.size})"
                isDeepHistoryMode -> "ПОШУК АКТУАЛЬНИХ ТЕНДЕРІВ (База: ${existingIds.size})"
                else -> "МОНІТОРИНГ СТРІЧКИ (База: ${existingIds.size})"
            }

            repository.addLog(
                "Старт пошуку ($modeName)",
                0,
                "Категорії: ${cpvPrefixes.joinToString(", ").ifEmpty { "Всі" }}"
            )

            var stopScan = false
            var matchingDuplicatesStreak = 0
            val newlyDiscoveredTenders = mutableListOf<Tender>()

            var newestDateModified = ""
            var oldestDateModified = ""

            var pagesProcessed = 0

            while (!stopScan) {
                if (ScanController.isCancellationRequested) {
                    stopScan = true
                    repository.addLog("Сканування зупинено", totalFound, "Зупинено користувачем на сторінці $pagesProcessed")
                    break
                }

                pagesProcessed++
                val feed = apiService.fetchFeed(url)
                if (feed == null || feed.data.isEmpty()) {
                    break
                }

                val items = feed.data
                totalExamined += items.size

                if (newestDateModified.isEmpty() && items.isNotEmpty()) {
                    newestDateModified = items.first().dateModified ?: ""
                }
                if (items.isNotEmpty()) {
                    oldestDateModified = items.last().dateModified ?: ""
                }

                // Check date bounds
                if (isCustomStart && items.isNotEmpty()) {
                    val lastDate = items.last().dateModified ?: ""
                    if (lastDate.isNotBlank() && lastDate <= startDateIso) {
                        stopScan = true
                    }
                } else if (isDeepHistoryMode && items.isNotEmpty()) {
                    // Deep/History mode reaches back 30 days
                    val lastDate = items.last().dateModified ?: ""
                    if (lastDate.isNotBlank() && lastDate <= thirtyDaysAgoIso) {
                        stopScan = true
                    }
                }

                val candidatesToFetch = mutableListOf<FeedItem>()

                for (item in items) {
                    if (ScanController.isCancellationRequested) {
                        stopScan = true
                        break
                    }

                    // Check if already in our database for this category
                    if (existingIds.contains(item.id)) {
                        matchingDuplicatesStreak++
                        if (!isDeepHistoryMode) {
                            if (matchingDuplicatesStreak >= currentStopThreshold) {
                                stopScan = true
                                break
                            }
                        }
                        continue
                    }

                    // Pre-filter: only skip statuses that are definitely closed / non-tendering
                    val st = item.status
                    if (!st.isNullOrBlank() && searchStatus == "active.tendering") {
                        val isDefinitelyClosed = st == "complete" || st == "unsuccessful" ||
                            st == "cancelled" || st == "active.qualification" ||
                            st == "active.awarded" || st == "active.auction"
                        if (isDefinitelyClosed) {
                            continue
                        }
                    }

                    candidatesToFetch.add(item)
                }

                if (stopScan && candidatesToFetch.isEmpty()) break

                // Concurrently fetch candidate details (12 parallel coroutines)
                if (candidatesToFetch.isNotEmpty()) {
                    val semaphore = Semaphore(12)
                    val deferredList = candidatesToFetch.map { candidate ->
                        async {
                            if (ScanController.isCancellationRequested) null
                            else semaphore.withPermit {
                                apiService.fetchTenderDetail(candidate.id)
                            }
                        }
                    }

                    val fullTenders = deferredList.awaitAll().filterNotNull()

                    // Filter matching tenders using status & CPV prefixes
                    val validRows = mutableListOf<Tender>()
                    for (tender in fullTenders) {
                        if (existingIds.contains(tender.id)) {
                            matchingDuplicatesStreak++
                            if (!isDeepHistoryMode && matchingDuplicatesStreak >= currentStopThreshold) {
                                stopScan = true
                            }
                            continue
                        }

                        // Check status
                        if (searchStatus.isNotBlank() && !searchStatus.equals("all", ignoreCase = true) && tender.status != searchStatus) {
                            continue
                        }

                        // Check CPV prefixes (with CpvMatcher hierarchical matching)
                        if (cpvPrefixes.isNotEmpty()) {
                            val tenderCpvs = tender.cpvCodes.split(",").map { it.trim().split("-")[0].trim() }
                            val match = tenderCpvs.any { tc ->
                                cpvPrefixes.any { prefix -> tc.startsWith(prefix) }
                            }
                            if (!match) continue
                        }

                        // Tender matches! Reset duplicate streak
                        matchingDuplicatesStreak = 0

                        val prepared = tender.copy(
                            emailStatus = if (isCustomStart || isDeepHistoryMode) "History" else "Pending",
                            foundAtTimestamp = System.currentTimeMillis()
                        )
                        validRows.add(prepared)
                    }

                    if (validRows.isNotEmpty()) {
                        repository.saveTenders(validRows)
                        validRows.forEach { existingIds.add(it.id) }
                        newlyDiscoveredTenders.addAll(validRows)
                        totalFound += validRows.size
                    }
                }

                val feedDate = oldestDateModified.take(16).replace("T", " ")
                repository.updateScanProgress(
                    ScanProgress(
                        examinedCount = totalExamined,
                        foundCount = totalFound,
                        currentFeedDate = feedDate,
                        statusText = "Перевірено: $totalExamined торгів • Знайдено: +$totalFound"
                    )
                )

                if (stopScan) break

                val nextUri = feed.next_page?.uri
                if (!nextUri.isNullOrBlank()) {
                    url = nextUri
                } else {
                    break
                }
            }

            val timeTakenSec = (System.currentTimeMillis() - startTime) / 1000
            val timeStr = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())

            val formattedDateRange = if (newestDateModified.isNotEmpty()) {
                val start = newestDateModified.take(16).replace("T", " ")
                val end = oldestDateModified.take(16).replace("T", " ")
                "$start → $end"
            } else {
                "Свіжа стрічка"
            }

            val wasStoppedManually = ScanController.isCancellationRequested
            val statusSummary = if (wasStoppedManually) {
                "Призупинено $timeStr: знайдено +$totalFound (перевірено $totalExamined)"
            } else {
                "Оновлено $timeStr: знайдено +$totalFound (перевірено $totalExamined)"
            }

            preferences.lastScanTimestamp = System.currentTimeMillis()
            preferences.lastScanStatus = statusSummary
            preferences.lastSyncDate = formattedDateRange

            repository.addLog(
                if (wasStoppedManually) "Сканування зупинено ($modeName)" else "Сканування завершено ($modeName)",
                totalFound,
                "Перевірено: $totalExamined, Нових: $totalFound ($timeTakenSec сек). Діапазон: $formattedDateRange"
            )

            // Notifications and email for newly found tenders (only in live monitor mode)
            if (newlyDiscoveredTenders.isNotEmpty() && !isCustomStart && !isDeepHistoryMode) {
                notificationHelper.showNewTendersNotification(newlyDiscoveredTenders)
                emailSender.sendTendersDigest(newlyDiscoveredTenders)
            }

            repository.refreshData()
            return@withContext ScanResult(
                success = true,
                newCount = totalFound,
                examinedCount = totalExamined,
                message = if (wasStoppedManually) "Сканування призупинено. Збережено +$totalFound торгів (перевірено $totalExamined)"
                          else "Знайдено +$totalFound торгів (перевірено $totalExamined). Діапазон: $formattedDateRange"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = "Помилка: ${e.localizedMessage ?: "Невідома помилка"}"
            preferences.lastScanStatus = "Помилка сканування: ${e.message}"
            repository.addLog("Помилка сканування", 0, msg)
            return@withContext ScanResult(
                success = false,
                newCount = 0,
                examinedCount = totalExamined,
                message = msg
            )
        } finally {
            isScanRunning = false
            ScanController.reset()
            repository.setScanning(false)
            repository.updateScanProgress(null)
            scanMutex.unlock()
        }
    }
}

data class ScanResult(
    val success: Boolean,
    val newCount: Int,
    val examinedCount: Int,
    val message: String
)
