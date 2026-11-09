package ua.gov.prozorro.radar.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import ua.gov.prozorro.radar.model.FeedResponse
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.model.TenderParser
import java.util.concurrent.TimeUnit

class ProzorroApiService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun fetchFeed(url: String, maxRetries: Int = 3): FeedResponse? = withContext(Dispatchers.IO) {
        var attempt = 0
        while (attempt < maxRetries) {
            attempt++
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "ProzorroRadar-Android/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@withContext null
                    return@withContext json.decodeFromString<FeedResponse>(bodyStr)
                } else if (response.code == 429) {
                    // Rate limit: exponential backoff
                    response.close()
                    delay(2000L * attempt)
                } else {
                    response.close()
                    if (attempt < maxRetries) {
                        delay(1000L * attempt)
                    }
                }
            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    e.printStackTrace()
                } else {
                    delay(1200L * attempt)
                }
            }
        }
        null
    }

    suspend fun fetchTenderDetail(id: String, maxRetries: Int = 3): Tender? = withContext(Dispatchers.IO) {
        var attempt = 0
        while (attempt < maxRetries) {
            attempt++
            try {
                val url = "$BASE_API_URL/$id"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "ProzorroRadar-Android/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@withContext null
                    val jsonEl = json.parseToJsonElement(bodyStr).jsonObject
                    val dataObj = jsonEl["data"]?.jsonObject ?: return@withContext null
                    return@withContext TenderParser.parseTender(dataObj)
                } else if (response.code == 429) {
                    response.close()
                    delay(1500L * attempt)
                } else {
                    response.close()
                    if (attempt < maxRetries) {
                        delay(1000L)
                    }
                }
            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    // Suppress stack trace on final retry
                } else {
                    delay(1000L)
                }
            }
        }
        null
    }

    companion object {
        const val BASE_API_URL = "https://public.api.openprocurement.org/api/2.5/tenders"
    }
}
