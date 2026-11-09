package ua.gov.prozorro.radar.network

import kotlinx.coroutines.Dispatchers
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

    suspend fun fetchFeed(url: String): FeedResponse? = withContext(Dispatchers.IO) {
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
                // Rate limit
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun fetchTenderDetail(id: String): Tender? = withContext(Dispatchers.IO) {
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    companion object {
        const val BASE_API_URL = "https://public.api.openprocurement.org/api/2.5/tenders"
    }
}
