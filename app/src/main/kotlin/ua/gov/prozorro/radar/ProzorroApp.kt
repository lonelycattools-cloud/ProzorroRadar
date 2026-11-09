package ua.gov.prozorro.radar

import android.app.Application
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.data.repository.TenderRepository
import ua.gov.prozorro.radar.service.NotificationHelper
import ua.gov.prozorro.radar.service.WorkScheduler

class ProzorroApp : Application() {

    lateinit var repository: TenderRepository
        private set

    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        repository = TenderRepository.getInstance(this)
        preferences = AppPreferences(this)

        // Initialize Notification Channels
        NotificationHelper(this)

        // Ensure background scanning schedule is active
        if (preferences.autoUpdateEnabled) {
            WorkScheduler.schedulePeriodicScan(this)
        }
    }

    companion object {
        lateinit var instance: ProzorroApp
            private set
    }
}
