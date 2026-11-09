package ua.gov.prozorro.radar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader

data class CpvItem(
    val code: String, // e.g. "18100000-0"
    val cleanCode: String, // e.g. "18100000"
    val prefix: String, // e.g. "1810"
    val name: String // e.g. "Формений одяг, спеціальний робочий одяг та аксесуари"
)

class CpvCatalog private constructor(private val context: Context) {

    private val allItems = mutableListOf<CpvItem>()
    private val codeToNameMap = mutableMapOf<String, String>()
    private var isLoaded = false

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        try {
            val assetManager = context.assets
            assetManager.open("dk021_uk.json").use { inputStream ->
                val reader = InputStreamReader(inputStream, Charsets.UTF_8)
                val jsonStr = reader.readText()
                val json = Json { ignoreUnknownKeys = true }
                val jsonObj = json.parseToJsonElement(jsonStr).jsonObject

                for ((code, nameEl) in jsonObj) {
                    val name = nameEl.jsonPrimitive.content
                    val cleanCode = code.split("-")[0].trim()
                    val prefix = cleanCode.trimEnd('0').let { if (it.length < 2) cleanCode.take(2) else it }
                    val item = CpvItem(
                        code = code,
                        cleanCode = cleanCode,
                        prefix = prefix,
                        name = name
                    )
                    allItems.add(item)
                    codeToNameMap[code] = name
                    codeToNameMap[cleanCode] = name
                    codeToNameMap[prefix] = name
                }
            }
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun search(query: String, limit: Int = 60): List<CpvItem> = withContext(Dispatchers.Default) {
        ensureLoaded()
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            // Return top-level divisions (codes ending in 000000-X)
            return@withContext allItems.filter { it.code.endsWith("000000-", ignoreCase = true) || it.code.endsWith("00000-") || it.code.endsWith("0000-") }
                .take(limit)
        }

        val exactMatches = mutableListOf<CpvItem>()
        val codeStarts = mutableListOf<CpvItem>()
        val nameContains = mutableListOf<CpvItem>()

        for (item in allItems) {
            val codeLower = item.code.lowercase()
            val cleanLower = item.cleanCode.lowercase()
            val nameLower = item.name.lowercase()

            if (cleanLower == q || codeLower == q) {
                exactMatches.add(item)
            } else if (cleanLower.startsWith(q) || codeLower.startsWith(q)) {
                codeStarts.add(item)
            } else if (nameLower.contains(q)) {
                nameContains.add(item)
            }

            if (exactMatches.size + codeStarts.size + nameContains.size >= limit * 2) {
                break
            }
        }

        (exactMatches + codeStarts + nameContains).distinctBy { it.cleanCode }.take(limit)
    }

    suspend fun getName(codeOrPrefix: String): String? = withContext(Dispatchers.Default) {
        ensureLoaded()
        val clean = codeOrPrefix.split("-")[0].trim()
        codeToNameMap[codeOrPrefix] ?: codeToNameMap[clean] ?: allItems.firstOrNull {
            it.code.startsWith(clean) || it.cleanCode.startsWith(clean)
        }?.name
    }

    companion object {
        @Volatile
        private var instance: CpvCatalog? = null

        fun getInstance(context: Context): CpvCatalog {
            return instance ?: synchronized(this) {
                instance ?: CpvCatalog(context.applicationContext).also { instance = it }
            }
        }
    }
}
