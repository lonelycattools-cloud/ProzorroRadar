package ua.gov.prozorro.radar.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ua.gov.prozorro.radar.data.AppPreferences
import java.util.concurrent.TimeUnit

object WorkScheduler {

    const val PERIODIC_WORK_TAG = "prozorro_periodic_scanner"
    const val ONE_TIME_WORK_TAG = "prozorro_one_time_scanner"

    fun schedulePeriodicScan(context: Context, forceUpdate: Boolean = false) {
        val prefs = AppPreferences(context)
        if (!prefs.autoUpdateEnabled) {
            cancelPeriodicScan(context)
            return
        }

        val interval = prefs.scanIntervalMinutes.coerceAtLeast(15) // WorkManager min is 15 minutes
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<ScanWorker>(
            interval.toLong(), TimeUnit.MINUTES,
            5L, TimeUnit.MINUTES // flex interval
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()

        val policy = if (forceUpdate) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            policy,
            periodicRequest
        )
    }

    fun triggerOneTimeScan(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<ScanWorker>()
            .setConstraints(constraints)
            .addTag(ONE_TIME_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun cancelPeriodicScan(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_TAG)
    }
}
