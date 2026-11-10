package ua.gov.prozorro.radar.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("prozorro_radar_prefs", Context.MODE_PRIVATE)

    var scanIntervalMinutes: Int
        get() = prefs.getInt(KEY_SCAN_INTERVAL, 30)
        set(value) = prefs.edit().putInt(KEY_SCAN_INTERVAL, value).apply()

    var cpvCodes: String
        get() = prefs.getString(KEY_CPV_CODES, "3023, 4500, 3360, 0913") ?: "3023, 4500, 3360, 0913"
        set(value) = prefs.edit().putString(KEY_CPV_CODES, value).apply()

    var searchStatus: String
        get() = prefs.getString(KEY_SEARCH_STATUS, "active.tendering") ?: "active.tendering"
        set(value) = prefs.edit().putString(KEY_SEARCH_STATUS, value).apply()

    var recipientEmails: String
        get() = prefs.getString(KEY_RECIPIENT_EMAILS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RECIPIENT_EMAILS, value).apply()

    var pushEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUSH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PUSH_ENABLED, value).apply()

    var dndNightModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DND_ENABLED, value).apply()

    var dndStartHour: Int
        get() = prefs.getInt(KEY_DND_START, 22)
        set(value) = prefs.edit().putInt(KEY_DND_START, value).apply()

    var dndEndHour: Int
        get() = prefs.getInt(KEY_DND_END, 8)
        set(value) = prefs.edit().putInt(KEY_DND_END, value).apply()

    var autoUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    var stopOnDuplicates: Int
        get() = prefs.getInt(KEY_STOP_ON_DUPLICATES, 5)
        set(value) = prefs.edit().putInt(KEY_STOP_ON_DUPLICATES, value).apply()

    var lastScanTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SCAN_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SCAN_TIME, value).apply()

    var lastScanStatus: String
        get() = prefs.getString(KEY_LAST_SCAN_STATUS, "Очікує запуску") ?: "Очікує запуску"
        set(value) = prefs.edit().putString(KEY_LAST_SCAN_STATUS, value).apply()

    var lastSyncOffset: String
        get() = prefs.getString(KEY_LAST_SYNC_OFFSET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_OFFSET, value).apply()

    var lastSyncDate: String
        get() = prefs.getString(KEY_LAST_SYNC_DATE, "Ще не встановлено") ?: "Ще не встановлено"
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_DATE, value).apply()

    fun resetSyncOffset() {
        prefs.edit().remove(KEY_LAST_SYNC_OFFSET).putString(KEY_LAST_SYNC_DATE, "Скинуто").apply()
    }

    var smtpHost: String
        get() {
            val v = prefs.getString(KEY_SMTP_HOST, null)
            return if (v.isNullOrBlank()) DEFAULT_SMTP_HOST else v
        }
        set(value) = prefs.edit().putString(KEY_SMTP_HOST, value).apply()

    var smtpPort: Int
        get() {
            val p = prefs.getInt(KEY_SMTP_PORT, DEFAULT_SMTP_PORT)
            return if (p <= 0) DEFAULT_SMTP_PORT else p
        }
        set(value) = prefs.edit().putInt(KEY_SMTP_PORT, value).apply()

    var smtpUser: String
        get() {
            val u = prefs.getString(KEY_SMTP_USER, null)
            return if (u.isNullOrBlank()) DEFAULT_SMTP_USER else u
        }
        set(value) = prefs.edit().putString(KEY_SMTP_USER, value).apply()

    var smtpPass: String
        get() {
            val p = prefs.getString(KEY_SMTP_PASS, null)
            return if (p.isNullOrBlank()) DEFAULT_SMTP_PASS else p
        }
        set(value) = prefs.edit().putString(KEY_SMTP_PASS, value).apply()

    var smtpUseTls: Boolean
        get() = prefs.getBoolean(KEY_SMTP_USE_TLS, true)
        set(value) = prefs.edit().putBoolean(KEY_SMTP_USE_TLS, value).apply()

    fun resetSmtpToDefault() {
        smtpHost = DEFAULT_SMTP_HOST
        smtpPort = DEFAULT_SMTP_PORT
        smtpUser = DEFAULT_SMTP_USER
        smtpPass = DEFAULT_SMTP_PASS
        smtpUseTls = true
    }

    fun isDefaultSmtp(): Boolean {
        return smtpUser.trim().equals(DEFAULT_SMTP_USER, ignoreCase = true)
    }

    var userContactEmail: String
        get() = prefs.getString(KEY_USER_CONTACT_EMAIL, "lonelycattools@gmail.com") ?: "lonelycattools@gmail.com"
        set(value) = prefs.edit().putString(KEY_USER_CONTACT_EMAIL, value).apply()

    var githubRepo: String
        get() = prefs.getString(KEY_GITHUB_REPO, "lonelycattools-cloud/ProzorroRadar") ?: "lonelycattools-cloud/ProzorroRadar"
        set(value) = prefs.edit().putString(KEY_GITHUB_REPO, value).apply()

    var forceUpdate: Boolean
        get() = prefs.getBoolean(KEY_FORCE_UPDATE, false)
        set(value) = prefs.edit().putBoolean(KEY_FORCE_UPDATE, value).apply()

    var isWizardCompleted: Boolean
        get() = prefs.getBoolean(KEY_WIZARD_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_WIZARD_COMPLETED, value).apply()

    fun getCleanCpvPrefixes(): List<String> {
        return cpvCodes.split(",")
            .map { it.trim().split("-")[0].trim() }
            .filter { it.isNotEmpty() }
            .map { clean ->
                val short = clean.trimEnd('0')
                if (short.length < 2) clean.take(2) else short
            }
    }

    companion object {
        const val DEFAULT_SMTP_HOST = "smtp.gmail.com"
        const val DEFAULT_SMTP_PORT = 587
        const val DEFAULT_SMTP_USER = "lonelycattools@gmail.com"
        const val DEFAULT_SMTP_PASS = "mfjvsfyzgtelifxd"

        private const val KEY_SCAN_INTERVAL = "scan_interval"
        private const val KEY_CPV_CODES = "cpv_codes"
        private const val KEY_SEARCH_STATUS = "search_status"
        private const val KEY_RECIPIENT_EMAILS = "recipient_emails"
        private const val KEY_PUSH_ENABLED = "push_enabled"
        private const val KEY_DND_ENABLED = "dnd_enabled"
        private const val KEY_DND_START = "dnd_start"
        private const val KEY_DND_END = "dnd_end"
        private const val KEY_AUTO_UPDATE = "auto_update"
        private const val KEY_STOP_ON_DUPLICATES = "stop_on_duplicates"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_LAST_SCAN_STATUS = "last_scan_status"
        private const val KEY_LAST_SYNC_OFFSET = "last_sync_offset"
        private const val KEY_LAST_SYNC_DATE = "last_sync_date"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USER = "smtp_user"
        private const val KEY_SMTP_PASS = "smtp_pass"
        private const val KEY_SMTP_USE_TLS = "smtp_use_tls"
        private const val KEY_USER_CONTACT_EMAIL = "user_contact_email"
        private const val KEY_GITHUB_REPO = "github_repo"
        private const val KEY_FORCE_UPDATE = "force_update"
        private const val KEY_WIZARD_COMPLETED = "wizard_completed"
    }
}
