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
            val cpvPrefixes = preferences.getCleanCpvPrefixes()
            val searchStatus = preferences.searchStatus.trim()

            // Existing IDs in SQLite for the CURRENT CPV categories
            val existingIds = repository.getExistingIds(cpvPrefixes).toMutableSet()

            val isCustomStart = !startDateIso.isNullOrBlank()
            // History mode: if we have fewer than 25 tenders for this category in DB
            val isHistoryMode = existingIds.size < 25

            // Stop threshold on duplicates of matching category tenders
            val currentStopThreshold = if (existingIds.isNotEmpty()) {
                minOf(preferences.stopOnDuplicates, existingIds.size)
            } else {
                999999
            }

            // Target number of tenders to collect in History mode
            val historyTargetCount = 30

            // Prozorro descending feed URL
            var url = "${ProzorroApiService.BASE_API_URL}?descending=1&limit=50&opt_fields=id,status,dateModified"

            val modeName = when {
                isCustomStart -> "З дати: ${startDateIso.take(10)}"
                isHistoryMode -> "ПОШУК ІСТОРІЇ (База: ${existingIds.size})"
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

            // Generous page limits:
            // Custom date: up to 300 pages (15,000 items)
            // History mode: up to 150 pages (7,500 items) or until 30 matching tenders found
            // Monitor mode: up to 100 pages (5,000 items) or until duplicate threshold reached
            val maxPages = if (isCustomStart) 300 else if (isHistoryMode) 150 else 100
            var pagesProcessed = 0

            while (!stopScan && pagesProcessed < maxPages) {
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

                // Check custom start date limit in descending feed:
                if (isCustomStart && items.isNotEmpty()) {
                    val lastDate = items.last().dateModified ?: ""
                    if (lastDate.isNotBlank() && lastDate <= startDateIso) {
                        stopScan = true
                    }
                }

                val candidatesToFetch = mutableListOf<FeedItem>()

                for (item in items) {
                    // Check if this tender is ALREADY in our database for this CPV category:
                    if (existingIds.contains(item.id)) {
                        matchingDuplicatesStreak++
                        if (!isHistoryMode && !isCustomStart) {
                            if (matchingDuplicatesStreak >= currentStopThreshold) {
                                stopScan = true
                                break
                            }
                        }
                        continue
                    }

                    // Pre-filter by status directly from feed item if specified
                    if (searchStatus.isNotBlank() && !searchStatus.equals("all", ignoreCase = true)) {
                        val st = item.status
                        if (!st.isNullOrBlank() && st != searchStatus) {
                            // Tender is complete, awarded, cancelled, etc. -> skip details fetch!
                            continue
                        }
                    }

                    candidatesToFetch.add(item)
                }

                if (stopScan && candidatesToFetch.isEmpty()) break

                // Concurrently fetch candidate details (20 parallel coroutines)
                if (candidatesToFetch.isNotEmpty()) {
                    val semaphore = Semaphore(20)
                    val deferredList = candidatesToFetch.map { candidate ->
                        async {
                            semaphore.withPermit {
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
                            if (!isHistoryMode && !isCustomStart && matchingDuplicatesStreak >= currentStopThreshold) {
                                stopScan = true
                            }
                            continue
                        }

                        // Check status again
                        if (searchStatus.isNotBlank() && !searchStatus.equals("all", ignoreCase = true) && tender.status != searchStatus) {
                            continue
                        }

                        // Check CPV prefixes
                        if (cpvPrefixes.isNotEmpty()) {
                            val tenderCpvs = tender.cpvCodes.split(",").map { it.trim().split("-")[0].trim() }
                            val match = tenderCpvs.any { tc ->
                                cpvPrefixes.any { prefix -> tc.startsWith(prefix) }
                            }
                            if (!match) continue
                        }

                        // Tender matches our criteria!
                        // Fresh matching tender resets the duplicate streak
                        matchingDuplicatesStreak = 0

                        val prepared = tender.copy(
                            emailStatus = if (isCustomStart || isHistoryMode) "History" else "Pending",
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

                // In history mode without custom date, stop once we've collected a solid batch of tenders
                if (isHistoryMode && !isCustomStart && totalFound >= historyTargetCount) {
                    stopScan = true
                    break
                }

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

            preferences.lastScanTimestamp = System.currentTimeMillis()
            preferences.lastScanStatus = "Оновлено $timeStr: знайдено +$totalFound (перевірено $totalExamined торгів)"
            preferences.lastSyncDate = formattedDateRange

            repository.addLog(
                "Сканування завершено ($modeName)",
                totalFound,
                "Перевірено: $totalExamined, Нових: $totalFound ($timeTakenSec сек). Діапазон: $formattedDateRange"
            )

            // Notifications and email for newly found tenders (only in live monitor mode)
            if (newlyDiscoveredTenders.isNotEmpty() && !isCustomStart && !isHistoryMode) {
                notificationHelper.showNewTendersNotification(newlyDiscoveredTenders)
                emailSender.sendTendersDigest(newlyDiscoveredTenders)
            }

            repository.refreshData()
            return@withContext ScanResult(
                success = true,
                newCount = totalFound,
                examinedCount = totalExamined,
                message = "Знайдено +$totalFound торгів (перевірено $totalExamined). Діапазон: $formattedDateRange"
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
