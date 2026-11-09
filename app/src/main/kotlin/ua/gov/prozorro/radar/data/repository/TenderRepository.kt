package ua.gov.prozorro.radar.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.data.db.ProzorroDbHelper
import ua.gov.prozorro.radar.model.DashboardMetrics
import ua.gov.prozorro.radar.model.ReportRow
import ua.gov.prozorro.radar.model.ScanLog
import ua.gov.prozorro.radar.model.Tender

class TenderRepository(context: Context) {

    private val dbHelper = ProzorroDbHelper(context)
    val preferences = AppPreferences(context)

    private val _tenders = MutableStateFlow<List<Tender>>(emptyList())
    val tenders: StateFlow<List<Tender>> = _tenders.asStateFlow()

    private val _metrics = MutableStateFlow(DashboardMetrics())
    val metrics: StateFlow<DashboardMetrics> = _metrics.asStateFlow()

    private val _logs = MutableStateFlow<List<ScanLog>>(emptyList())
    val logs: StateFlow<List<ScanLog>> = _logs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    suspend fun refreshData(
        query: String = "",
        statusFilter: String = "",
        favoritesOnly: Boolean = false,
        filterByActiveCpvs: Boolean = true
    ) {
        withContext(Dispatchers.IO) {
            val cpvPrefixes = if (filterByActiveCpvs) preferences.getCleanCpvPrefixes() else emptyList()
            val list = dbHelper.getAllTenders(query, statusFilter, favoritesOnly, cpvPrefixes)
            _tenders.value = list
            refreshMetrics(cpvPrefixes)
            refreshLogs()
        }
    }

    suspend fun refreshMetrics(cpvPrefixes: List<String> = preferences.getCleanCpvPrefixes()) {
        withContext(Dispatchers.IO) {
            val m = dbHelper.getMetrics(cpvPrefixes).copy(
                lastScanTime = preferences.lastScanTimestamp,
                lastScanStatus = preferences.lastScanStatus,
                lastSyncDate = preferences.lastSyncDate
            )
            _metrics.value = m
        }
    }

    suspend fun refreshLogs() {
        withContext(Dispatchers.IO) {
            _logs.value = dbHelper.getLogs(50)
        }
    }

    suspend fun toggleFavorite(id: String, current: Boolean) {
        withContext(Dispatchers.IO) {
            dbHelper.toggleFavorite(id, current)
            refreshData()
        }
    }

    suspend fun saveTenders(tenders: List<Tender>): Int {
        return withContext(Dispatchers.IO) {
            var count = 0
            for (t in tenders) {
                if (dbHelper.insertOrUpdateTender(t)) {
                    count++
                }
            }
            if (count > 0) {
                refreshData()
            }
            count
        }
    }

    suspend fun getExistingIds(cpvPrefixes: List<String> = emptyList()): Set<String> {
        return withContext(Dispatchers.IO) {
            dbHelper.getExistingIds(cpvPrefixes)
        }
    }

    suspend fun addLog(event: String, count: Int = 0, details: String = "") {
        withContext(Dispatchers.IO) {
            dbHelper.insertLog(event, count, details)
            refreshLogs()
        }
    }

    fun setScanning(active: Boolean) {
        _isScanning.value = active
    }

    suspend fun clearAllTenders() {
        withContext(Dispatchers.IO) {
            dbHelper.clearAllTenders()
            refreshData()
        }
    }

    // Analytics reports
    suspend fun getTopProcuringEntities(limit: Int = 10): List<ReportRow> = withContext(Dispatchers.IO) {
        val cpvs = preferences.getCleanCpvPrefixes()
        dbHelper.getTopProcuringEntities(limit, cpvs)
    }

    suspend fun getTopCpvCategories(limit: Int = 10): List<ReportRow> = withContext(Dispatchers.IO) {
        val cpvs = preferences.getCleanCpvPrefixes()
        dbHelper.getTopCpvCategories(limit, cpvs)
    }

    suspend fun getTendersByEdrpou(edrpou: String) = withContext(Dispatchers.IO) {
        dbHelper.getTendersByEdrpou(edrpou)
    }

    suspend fun getTendersByCpvAndPeriod(cpvPrefix: String, fromTime: Long, toTime: Long) = withContext(Dispatchers.IO) {
        dbHelper.getTendersByCpvAndPeriod(cpvPrefix, fromTime, toTime)
    }

    suspend fun getAllTendersForExport(): List<Tender> = withContext(Dispatchers.IO) {
        dbHelper.getAllTenders(limit = 10000)
    }

    companion object {
        @Volatile
        private var instance: TenderRepository? = null

        fun getInstance(context: Context): TenderRepository {
            return instance ?: synchronized(this) {
                instance ?: TenderRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
