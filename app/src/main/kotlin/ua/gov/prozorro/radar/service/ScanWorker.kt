package ua.gov.prozorro.radar.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ua.gov.prozorro.radar.network.ProzorroScannerEngine

class ScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (ProzorroScannerEngine.isScanRunning) {
            return Result.success()
        }
        val scanner = ProzorroScannerEngine(applicationContext)
        val result = scanner.scanNow()
        return if (result.success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
