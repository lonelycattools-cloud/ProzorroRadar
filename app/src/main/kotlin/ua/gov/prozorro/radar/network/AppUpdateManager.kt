package ua.gov.prozorro.radar.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import ua.gov.prozorro.radar.BuildConfig
import ua.gov.prozorro.radar.data.AppPreferences
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubRelease(
    val tag_name: String = "",
    val name: String? = null,
    val body: String? = null,
    val published_at: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    val size: Long = 0L,
    val browser_download_url: String = "",
    val content_type: String = ""
)

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSizeMb: Double,
    val isForced: Boolean,
    val hasUpdate: Boolean
)

class AppUpdateManager(private val context: Context) {

    private val preferences = AppPreferences(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val repo = preferences.githubRepo.trim()
            if (repo.isBlank() || !repo.contains("/")) {
                return@withContext Result.failure(IllegalArgumentException("Некоректний репозиторій GitHub (має бути format owner/repo)"))
            }

            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ProzorroRadar-Android")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext Result.failure(Exception("Релізи у репозиторії $repo не знайдені (404)"))
                }
                return@withContext Result.failure(Exception("Помилка GitHub API (${response.code}): ${response.message}"))
            }

            val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Порожня відповідь GitHub"))
            val release = json.decodeFromString<GitHubRelease>(bodyStr)

            val rawTag = release.tag_name.trim().removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME

            // Find APK asset
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apkAsset?.browser_download_url ?: ""
            val apkSizeMb = if (apkAsset != null) (apkAsset.size.toDouble() / (1024 * 1024)) else 0.0

            val hasUpdate = isNewerVersion(remoteVersion = rawTag, currentVersion = currentVersion)
            val releaseNotes = release.body?.trim() ?: "Оновлення стабільності та виправлення помилок."
            val isForced = preferences.forceUpdate || releaseNotes.contains("[FORCED]", ignoreCase = true) || release.name?.contains("[FORCED]", ignoreCase = true) == true

            val info = UpdateInfo(
                currentVersion = currentVersion,
                latestVersion = release.tag_name,
                releaseTitle = release.name ?: "Версія ${release.tag_name}",
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                apkSizeMb = apkSizeMb,
                isForced = isForced,
                hasUpdate = hasUpdate
            )

            Result.success(info)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        versionTag: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "ProzorroRadar-Android")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Помилка завантаження (${response.code})"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Порожнє тіло файлу оновлення"))
            val totalBytes = body.contentLength()

            val dir = File(context.cacheDir, "updates")
            if (!dir.exists()) dir.mkdirs()
            val cleanTag = versionTag.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val targetFile = File(dir, "ProzorroRadar_$cleanTag.apk")

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun promptInstallApk(apkFile: File): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                    return Result.failure(Exception("Надайте дозвіл на встановлення невідомих додатків у налаштуваннях"))
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return remoteVersion != currentVersion
        }
    }
}
