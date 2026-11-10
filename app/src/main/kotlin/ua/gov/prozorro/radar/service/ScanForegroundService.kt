package ua.gov.prozorro.radar.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.network.ProzorroScannerEngine

class ScanForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentScanJob: kotlinx.coroutines.Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ProzorroRadar::ScanForegroundServiceWakeLock"
            ).apply {
                acquire(25 * 60 * 1000L) // 25 minutes safety cap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SCAN

        if (action == ACTION_STOP_SCAN) {
            val engine = ProzorroScannerEngine(applicationContext)
            engine.requestStop()
            currentScanJob?.cancel()
            stopForegroundService()
            return START_NOT_STICKY
        }

        if (ProzorroScannerEngine.isScanRunning || currentScanJob?.isActive == true) {
            return START_NOT_STICKY
        }

        val startDateIso = intent?.getStringExtra(EXTRA_START_DATE_ISO)
        val deepScan = intent?.getBooleanExtra(EXTRA_DEEP_SCAN, false) ?: false
        val resetCheckpoint = intent?.getBooleanExtra(EXTRA_RESET_CHECKPOINT, false) ?: false

        val helper = NotificationHelper(this)
        val stopIntent = Intent(this, ScanForegroundService::class.java).apply {
            this.action = ACTION_STOP_SCAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = helper.buildScanningNotification(stopPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_SCANNING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_SCANNING, notification)
        }

        currentScanJob = serviceScope.launch {
            try {
                val engine = ProzorroScannerEngine(applicationContext)
                engine.scanNow(
                    startDateIso = startDateIso,
                    deepScan = deepScan,
                    resetCheckpoint = resetCheckpoint
                )
            } finally {
                stopForegroundService()
            }
        }

        return START_NOT_STICKY
    }

    private fun stopForegroundService() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        stopForegroundService()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_SCAN = "ua.gov.prozorro.radar.action.START_SCAN"
        const val ACTION_STOP_SCAN = "ua.gov.prozorro.radar.action.STOP_SCAN"
        const val EXTRA_START_DATE_ISO = "extra_start_date_iso"
        const val EXTRA_DEEP_SCAN = "extra_deep_scan"
        const val EXTRA_RESET_CHECKPOINT = "extra_reset_checkpoint"

        fun startScan(
            context: Context,
            startDateIso: String? = null,
            deepScan: Boolean = false,
            resetCheckpoint: Boolean = false
        ) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_START_SCAN
                putExtra(EXTRA_START_DATE_ISO, startDateIso)
                putExtra(EXTRA_DEEP_SCAN, deepScan)
                putExtra(EXTRA_RESET_CHECKPOINT, resetCheckpoint)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopScan(context: Context) {
            val intent = Intent(context, ScanForegroundService::class.java).apply {
                action = ACTION_STOP_SCAN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
