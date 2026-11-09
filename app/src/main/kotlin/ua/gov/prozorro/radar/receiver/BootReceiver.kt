package ua.gov.prozorro.radar.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.service.WorkScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = AppPreferences(context)
            if (prefs.autoUpdateEnabled) {
                WorkScheduler.schedulePeriodicScan(context, forceUpdate = true)
            }
        }
    }
}
