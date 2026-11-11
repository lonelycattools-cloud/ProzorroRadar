package ua.gov.prozorro.radar.ui.main

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.data.repository.TenderRepository
import ua.gov.prozorro.radar.export.ExcelExporter
import ua.gov.prozorro.radar.model.DashboardMetrics
import ua.gov.prozorro.radar.model.ReportResult
import ua.gov.prozorro.radar.model.ReportRow
import ua.gov.prozorro.radar.model.ScanProgress
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.network.ProzorroScannerEngine
import ua.gov.prozorro.radar.service.ScanForegroundService

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TenderRepository.getInstance(application)
    val preferences = AppPreferences(application)
    private val scannerEngine = ProzorroScannerEngine(application)

    val tenders: StateFlow<List<Tender>> = repository.tenders
    val metrics: StateFlow<DashboardMetrics> = repository.metrics
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val scanProgress: StateFlow<ScanProgress?> = repository.scanProgress

    private val _topEntities = MutableStateFlow<List<ReportRow>>(emptyList())
    val topEntities: StateFlow<List<ReportRow>> = _topEntities.asStateFlow()

    private val _topCpvs = MutableStateFlow<List<ReportRow>>(emptyList())
    val topCpvs: StateFlow<List<ReportRow>> = _topCpvs.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            repository.isScanning.collect { scanning ->
                if (!scanning) {
                    _topEntities.value = repository.getTopProcuringEntities()
                    _topCpvs.value = repository.getTopCpvCategories()
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            repository.refreshData()
            _topEntities.value = repository.getTopProcuringEntities()
            _topCpvs.value = repository.getTopCpvCategories()
        }
    }

    fun scanNow(deepScan: Boolean = false, resetCheckpoint: Boolean = false) {
        ScanForegroundService.startScan(
            context = getApplication(),
            startDateIso = null,
            deepScan = deepScan,
            resetCheckpoint = resetCheckpoint
        )
        Toast.makeText(getApplication(), "Сканування запущено у фоні", Toast.LENGTH_SHORT).show()
    }

    fun stopScan() {
        ScanForegroundService.stopScan(getApplication())
        scannerEngine.requestStop()
        Toast.makeText(getApplication(), "Зупинка сканування...", Toast.LENGTH_SHORT).show()
    }

    fun scanFromDate(startDateIso: String, cpvCodes: String? = null, resetCheckpoint: Boolean = false) {
        if (!cpvCodes.isNullOrBlank()) {
            preferences.cpvCodes = cpvCodes
        }
        ScanForegroundService.startScan(
            context = getApplication(),
            startDateIso = startDateIso,
            deepScan = true,
            resetCheckpoint = resetCheckpoint
        )
        Toast.makeText(getApplication(), "Сканування з дати запущено у фоні", Toast.LENGTH_SHORT).show()
    }

    fun clearAllTenders() {
        viewModelScope.launch {
            repository.clearAllTenders()
            _topEntities.value = emptyList()
            _topCpvs.value = emptyList()
            Toast.makeText(getApplication(), "Базу даних торгів очищено", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetSyncOffset() {
        viewModelScope.launch {
            preferences.resetSyncOffset()
            repository.refreshMetrics()
            Toast.makeText(getApplication(), "Точку зупинки скинуто. Наступне сканування почнеться зі свіжої стрічки.", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleFavorite(id: String, current: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, current)
        }
    }

    fun exportAllTenders(context: Context) {
        viewModelScope.launch {
            val list = repository.getAllTendersForExport()
            if (list.isEmpty()) {
                Toast.makeText(context, "Немає збережених торгів для експорту", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val res = ExcelExporter.exportTendersToExcel(context, list)
            if (res.isSuccess) {
                ExcelExporter.shareExcelFile(context, res.getOrThrow(), "Вивантаження тендерів в Excel")
            } else {
                Toast.makeText(context, "Помилка експорту: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportReport(context: Context, report: ReportResult) {
        viewModelScope.launch {
            val res = ExcelExporter.exportReportToExcel(context, report)
            if (res.isSuccess) {
                ExcelExporter.shareExcelFile(context, res.getOrThrow(), "Вивантаження звіту в Excel")
            } else {
                Toast.makeText(context, "Помилка експорту: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
