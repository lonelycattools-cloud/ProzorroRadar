package ua.gov.prozorro.radar.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.data.repository.TenderRepository
import ua.gov.prozorro.radar.model.FeedItem
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.service.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProzorroScannerEngine(private val context: Context) {

    private val apiService = ProzorroApiService()
    private val repository = TenderRepository.getInstance(context)
    private val preferences = AppPreferences(context)
    private val notificationHelper = NotificationHelper(context)
    private val emailSender = EmailSender(context)

    suspend fun scanNow(startDateIso: String? = null): ScanResult = withContext(Dispatchers.IO) {
        repository.setScanning(true)
        val startTime = System.currentTimeMillis()
        var totalFound = 0
        var totalExamined = 0

        try {
            val existingIds = repository.getExistingIds().toMutableSet()
            val cpvPrefixes = preferences.getCleanCpvPrefixes()
            val searchStatus = preferences.searchStatus.trim()

            // Determine synchronization mode and initial URL:
            val isCustomStart = !startDateIso.isNullOrBlank()
            val savedOffset = preferences.lastSyncOffset

            var url: String
            val isForwardSync: Boolean

            if (isCustomStart) {
                // User explicitly requested starting from a specific date forward to now
                url = "${ProzorroApiService.BASE_API_URL}?offset=${startDateIso}&limit=50&opt_fields=id,status,dateModified"
                isForwardSync = true
                val displayStart = startDateIso.take(16).replace("T", " ")
                repository.addLog("Старт з дати: $displayStart", 0, "Сканування до теперішнього часу")
            } else if (savedOffset.isNotBlank()) {
                // Resume from previously saved checkpoint!
                url = "${ProzorroApiService.BASE_API_URL}?offset=$savedOffset&limit=50&opt_fields=id,status,dateModified"
                isForwardSync = true
                repository.addLog("Продовження з точки зупинки", 0, "Точка: ${preferences.lastSyncDate}")
            } else {
                // First run without offset: descending feed
                url = "${ProzorroApiService.BASE_API_URL}?descending=1&limit=50&opt_fields=id,status,dateModified"
                isForwardSync = false
                repository.addLog("Початкове сканування (Свіжа стрічка)", 0, "База: ${existingIds.size} торгів")
            }

            var stopScan = false
            var duplicatesStreak = 0
            val newlyDiscoveredTenders = mutableListOf<Tender>()
            var lastCheckpointOffset = savedOffset
            var lastCheckpointDate = preferences.lastSyncDate

            var pagesProcessed = 0
            val maxPages = if (isCustomStart) 100 else 40 // prevent infinite loops on very old dates

            while (!stopScan && pagesProcessed < maxPages) {
                pagesProcessed++
                val feed = apiService.fetchFeed(url)
                if (feed == null || feed.data.isEmpty()) {
                    // Feed finished (reached the present moment or empty)
                    break
                }

                val items = feed.data
                totalExamined += items.size
                val candidates = mutableListOf<FeedItem>()

                for (item in items) {
                    if (existingIds.contains(item.id)) {
                        if (!isForwardSync) {
                            duplicatesStreak++
                            if (duplicatesStreak >= preferences.stopOnDuplicates) {
                                stopScan = true
                                break
                            }
                        }
                    } else {
                        duplicatesStreak = 0
                        candidates.add(item)
                    }
                }

                if (stopScan && candidates.isEmpty()) break

                // Concurrently fetch candidate details (limit to 12 workers)
                if (candidates.isNotEmpty()) {
                    val semaphore = Semaphore(12)
                    val deferredList = candidates.map { candidate ->
                        async {
                            semaphore.withPermit {
                                apiService.fetchTenderDetail(candidate.id)
                            }
                        }
                    }

                    val fullTenders = deferredList.awaitAll().filterNotNull()

                    // Filter matching tenders
                    val validRows = mutableListOf<Tender>()
                    for (tender in fullTenders) {
                        if (existingIds.contains(tender.id)) continue

                        // Check status
                        if (searchStatus.isNotEmpty() && tender.status != searchStatus) {
                            continue
                        }

                        // Check CPV
                        if (cpvPrefixes.isNotEmpty()) {
                            val tenderCpvs = tender.cpvCodes.split(",").map { it.trim() }
                            val match = tenderCpvs.any { tc ->
                                cpvPrefixes.any { prefix -> tc.startsWith(prefix) }
                            }
                            if (!match) continue
                        }

                        val prepared = tender.copy(
                            emailStatus = if (isCustomStart) "History" else "Pending",
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

                // Update checkpoint offset
                val nextOffset = feed.next_page?.offset
                if (!nextOffset.isNullOrBlank()) {
                    lastCheckpointOffset = nextOffset
                }

                if (items.isNotEmpty()) {
                    val latestDate = items.lastOrNull()?.dateModified ?: items.first().dateModified
                    if (!latestDate.isNullOrBlank()) {
                        lastCheckpointDate = latestDate.take(16).replace("T", " ")
                    }
                }

                if (stopScan) break

                val nextUri = feed.next_page?.uri
                if (!nextUri.isNullOrBlank()) {
                    url = nextUri
                } else {
                    break
                }
            }

            // Save new stopping point (Offset & Human date)
            if (lastCheckpointOffset.isNotBlank()) {
                preferences.lastSyncOffset = lastCheckpointOffset
            }
            if (lastCheckpointDate.isNotBlank()) {
                preferences.lastSyncDate = lastCheckpointDate
            }

            val timeTakenSec = (System.currentTimeMillis() - startTime) / 1000
            val timeStr = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())
            preferences.lastScanTimestamp = System.currentTimeMillis()
            preferences.lastScanStatus = "Оновлено $timeStr (Точка: $lastCheckpointDate, знайдено: $totalFound)"

            repository.addLog(
                "Сканування завершено",
                totalFound,
                "Перевірено: $totalExamined, Нових: $totalFound ($timeTakenSec сек). Точка: $lastCheckpointDate"
            )

            // Notifications and email for newly found tenders
            if (newlyDiscoveredTenders.isNotEmpty() && !isCustomStart) {
                notificationHelper.showNewTendersNotification(newlyDiscoveredTenders)
                emailSender.sendTendersDigest(newlyDiscoveredTenders)
            }

            repository.refreshMetrics()
            return@withContext ScanResult(
                success = true,
                newCount = totalFound,
                examinedCount = totalExamined,
                message = "Успішно. Знайдено $totalFound торгів. Точка зупинки: $lastCheckpointDate"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            preferences.lastScanStatus = "Помилка сканування: ${e.message}"
            repository.addLog("Помилка сканування", 0, e.message ?: "Невідома помилка")
            return@withContext ScanResult(
                success = false,
                newCount = 0,
                examinedCount = totalExamined,
                message = e.message ?: "Помилка"
            )
        } finally {
            repository.setScanning(false)
        }
    }
}

data class ScanResult(
    val success: Boolean,
    val newCount: Int,
    val examinedCount: Int,
    val message: String
)
