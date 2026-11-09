package ua.gov.prozorro.radar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ua.gov.prozorro.radar.MainActivity
import ua.gov.prozorro.radar.R
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.model.Tender
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    private val preferences = AppPreferences(context)
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelNormal = NotificationChannel(
                CHANNEL_TENDERS,
                "Нові тендери Prozorro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про знайдені закупівлі за вашими критеріями"
                enableVibration(true)
            }

            val channelSilent = NotificationChannel(
                CHANNEL_SILENT,
                "Нічні сповіщення (Без звуку)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Беззвучні сповіщення в період режиму «Не турбувати»"
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(channelNormal)
            notificationManager.createNotificationChannel(channelSilent)
        }
    }

    fun isQuietHours(): Boolean {
        if (!preferences.dndNightModeEnabled) return false
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val start = preferences.dndStartHour // default 22
        val end = preferences.dndEndHour // default 8

        return if (start > end) {
            // e.g. 22:00 to 08:00
            currentHour >= start || currentHour < end
        } else {
            // e.g. 01:00 to 06:00
            currentHour in start until end
        }
    }

    fun showNewTendersNotification(newTenders: List<Tender>) {
        if (!preferences.pushEnabled || newTenders.isEmpty()) return

        val count = newTenders.size
        val latest = newTenders.first()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TENDER_ID", latest.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inQuietHours = isQuietHours()
        val channelId = if (inQuietHours) CHANNEL_SILENT else CHANNEL_TENDERS

        val title = if (count == 1) {
            "Знайдено новий тендер!"
        } else {
            "Знайдено $count нових тендерів!"
        }

        val contentText = "${latest.formattedBudget} • ${latest.procuringEntityName}"
        val bigText = buildString {
            append(latest.title)
            append("\nЗамовник: ${latest.procuringEntityName}")
            if (latest.procuringEntityEdrpou.isNotEmpty()) {
                append(" (ЄДРПОУ: ${latest.procuringEntityEdrpou})")
            }
            append("\nБюджет: ${latest.formattedBudget}")
            append("\nДедлайн: ${latest.deadline}")
            if (count > 1) {
                append("\nта ще ${count - 1} інших торгів у вашій вибірці.")
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (inQuietHours) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TENDERS, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_TENDERS = "prozorro_tenders_alerts"
        const val CHANNEL_SILENT = "prozorro_tenders_silent"
        const val NOTIFICATION_ID_TENDERS = 1001
    }
}
