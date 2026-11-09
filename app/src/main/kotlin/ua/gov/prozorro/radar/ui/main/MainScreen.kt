package ua.gov.prozorro.radar.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.gov.prozorro.radar.ui.archive.ArchiveScreen
import ua.gov.prozorro.radar.ui.components.PermissionsOnboardingDialog
import ua.gov.prozorro.radar.ui.components.UpdateDialog
import ua.gov.prozorro.radar.ui.dashboard.DashboardScreen
import ua.gov.prozorro.radar.ui.help.HelpScreen
import ua.gov.prozorro.radar.ui.reports.ReportsScreen
import ua.gov.prozorro.radar.ui.settings.SettingsScreen
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Головна", Icons.Default.Dashboard),
    ARCHIVE("Архів", Icons.Default.Folder),
    REPORTS("Звіти", Icons.Default.Assessment),
    SETTINGS("Налаштування", Icons.Default.Settings),
    HELP("Довідка", Icons.Default.HelpOutline)
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    val tenders by viewModel.tenders.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val topEntities by viewModel.topEntities.collectAsStateWithLifecycle()
    val topCpvs by viewModel.topCpvs.collectAsStateWithLifecycle()

    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedApkFile by viewModel.downloadedApkFile.collectAsStateWithLifecycle()

    var showPermissionsDialog by remember { mutableStateOf(false) }

    // Request Notification permission (Android 13+)
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // handled
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("prozorro_radar_app", Context.MODE_PRIVATE)
        val hasShown = prefs.getBoolean("permissions_onboarding_shown", false)
        if (!hasShown) {
            showPermissionsDialog = true
            prefs.edit().putBoolean("permissions_onboarding_shown", true).apply()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { 
                            currentTab = tab 
                            if (tab == AppTab.DASHBOARD || tab == AppTab.ARCHIVE || tab == AppTab.REPORTS) {
                                viewModel.loadData()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> {
                    DashboardScreen(
                        metrics = metrics,
                        tenders = tenders,
                        isScanning = isScanning,
                        scanProgress = scanProgress,
                        currentCpvCodes = viewModel.preferences.cpvCodes,
                        onScanNow = { viewModel.scanNow(deepScan = false) },
                        onDeepScan = { resetCheckpoint -> viewModel.scanNow(deepScan = true, resetCheckpoint = resetCheckpoint) },
                        onStopScan = { viewModel.stopScan() },
                        onScanFromDate = { dateIso, cpvs, resetCheckpoint -> viewModel.scanFromDate(dateIso, cpvs, resetCheckpoint) },
                        onResetCheckpoint = { viewModel.resetSyncOffset() },
                        onToggleFavorite = { id, curr -> viewModel.toggleFavorite(id, curr) },
                        onNavigateToArchive = { currentTab = AppTab.ARCHIVE },
                        onExportAll = { viewModel.exportAllTenders(context) }
                    )
                }
                AppTab.ARCHIVE -> {
                    ArchiveScreen(
                        tenders = tenders,
                        onToggleFavorite = { id, curr -> viewModel.toggleFavorite(id, curr) },
                        onExportExcel = { viewModel.exportAllTenders(context) }
                    )
                }
                AppTab.REPORTS -> {
                    ReportsScreen(
                        tenders = tenders,
                        topEntities = topEntities,
                        topCpvs = topCpvs,
                        onExportReport = { report -> viewModel.exportReport(context, report) }
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        preferences = viewModel.preferences,
                        isCheckingUpdate = isCheckingUpdate,
                        onCheckUpdate = { viewModel.checkForUpdate(isManual = true) },
                        onResetSyncOffset = { viewModel.resetSyncOffset() },
                        onCategoriesChanged = { viewModel.loadData() },
                        onClearDatabase = { viewModel.clearAllTenders() }
                    )
                }
                AppTab.HELP -> {
                    HelpScreen()
                }
            }
        }
    }

    if (showPermissionsDialog) {
        PermissionsOnboardingDialog(
            onDismiss = { showPermissionsDialog = false },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onRequestBatteryOptimization = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }

    updateInfo?.let { info ->
        UpdateDialog(
            updateInfo = info,
            downloadProgress = downloadProgress,
            isReadyToInstall = downloadedApkFile != null,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onStartDownload = { viewModel.startUpdateDownload() },
            onInstall = { viewModel.installDownloadedApk() }
        )
    }
}
