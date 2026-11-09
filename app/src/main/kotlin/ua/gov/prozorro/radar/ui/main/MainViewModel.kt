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
import ua.gov.prozorro.radar.network.AppUpdateManager
import ua.gov.prozorro.radar.network.ProzorroScannerEngine
import ua.gov.prozorro.radar.network.UpdateInfo
import ua.gov.prozorro.radar.service.ScanForegroundService
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TenderRepository.getInstance(application)
    val preferences = AppPreferences(application)
    private val scannerEngine = ProzorroScannerEngine(application)
    private val updateManager = AppUpdateManager(application)

    val tenders: StateFlow<List<Tender>> = repository.tenders
    val metrics: StateFlow<DashboardMetrics> = repository.metrics
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val scanProgress: StateFlow<ScanProgress?> = repository.scanProgress

    private val _topEntities = MutableStateFlow<List<ReportRow>>(emptyList())
    val topEntities: StateFlow<List<ReportRow>> = _topEntities.asStateFlow()

    private val _topCpvs = MutableStateFlow<List<ReportRow>>(emptyList())
    val topCpvs: StateFlow<List<ReportRow>> = _topCpvs.asStateFlow()

    // Update state
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _downloadedApkFile = MutableStateFlow<File?>(null)
    val downloadedApkFile: StateFlow<File?> = _downloadedApkFile.asStateFlow()

    init {
        loadData()
        if (preferences.autoUpdateEnabled) {
            checkForUpdate(isManual = false)
        }
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

    // GitHub Updates
    fun checkForUpdate(isManual: Boolean = false) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = updateManager.checkForUpdates()
            _isCheckingUpdate.value = false

            result.fold(
                onSuccess = { info ->
                    if (info != null && info.hasUpdate) {
                        _updateInfo.value = info
                    } else if (isManual) {
                        Toast.makeText(getApplication(), "Встановлено найновішу версію (${info?.currentVersion ?: "1.0"})", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { err ->
                    if (isManual) {
                        Toast.makeText(getApplication(), "Перевірка оновлень: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    fun startUpdateDownload() {
        val info = _updateInfo.value ?: return
        if (info.downloadUrl.isBlank()) {
            Toast.makeText(getApplication(), "Файл APK відсутній у цьому релізі на GitHub", Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            _downloadProgress.value = 0.01f
            val res = updateManager.downloadApk(
                downloadUrl = info.downloadUrl,
                versionTag = info.latestVersion,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )
            res.fold(
                onSuccess = { file ->
                    _downloadProgress.value = null
                    _downloadedApkFile.value = file
                    installDownloadedApk()
                },
                onFailure = { err ->
                    _downloadProgress.value = null
                    Toast.makeText(getApplication(), "Помилка завантаження: ${err.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    fun installDownloadedApk() {
        val file = _downloadedApkFile.value ?: return
        val res = updateManager.promptInstallApk(file)
        if (res.isFailure) {
            Toast.makeText(getApplication(), res.exceptionOrNull()?.message ?: "Помилка встановлення", Toast.LENGTH_LONG).show()
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
        _downloadProgress.value = null
        _downloadedApkFile.value = null
    }
}
