package ua.gov.prozorro.radar.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class FeedResponse(
    val data: List<FeedItem> = emptyList(),
    val next_page: NextPage? = null
)

@Serializable
data class FeedItem(
    val id: String,
    val dateModified: String? = null,
    val status: String? = null
)

@Serializable
data class NextPage(
    val uri: String? = null,
    val offset: String? = null
)

@Serializable
data class TenderDetailResponse(
    val data: JsonObject
)

object TenderParser {
    fun parseTender(data: JsonObject): Tender {
        val id = data["id"]?.jsonPrimitive?.contentOrNull ?: ""
        val tenderID = data["tenderID"]?.jsonPrimitive?.contentOrNull ?: id
        val title = data["title"]?.jsonPrimitive?.contentOrNull ?: "Без назви"
        val description = data["description"]?.jsonPrimitive?.contentOrNull ?: ""
        val status = data["status"]?.jsonPrimitive?.contentOrNull ?: "unknown"
        val dateModified = data["dateModified"]?.jsonPrimitive?.contentOrNull ?: ""

        val valueObj = data["value"]?.jsonObject
        val amount = valueObj?.get("amount")?.jsonPrimitive?.doubleOrNull ?: 0.0
        val currency = valueObj?.get("currency")?.jsonPrimitive?.contentOrNull ?: "UAH"

        val entityObj = data["procuringEntity"]?.jsonObject
        val entityName = entityObj?.get("name")?.jsonPrimitive?.contentOrNull ?: "-"
        val identifierObj = entityObj?.get("identifier")?.jsonObject
        val edrpou = identifierObj?.get("id")?.jsonPrimitive?.contentOrNull ?: ""

        // Extract CPVs from classification and items
        val cpvSet = mutableSetOf<String>()
        val mainClass = data["classification"]?.jsonObject
        mainClass?.get("id")?.jsonPrimitive?.contentOrNull?.let { cpvSet.add(it) }

        data["additionalClassifications"]?.jsonArray?.forEach { acEl ->
            acEl.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.let { cpvSet.add(it) }
        }

        data["items"]?.jsonArray?.forEach { itemEl ->
            val itemObj = itemEl.jsonObject
            itemObj["classification"]?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull?.let {
                cpvSet.add(it)
            }
            itemObj["additionalClassifications"]?.jsonArray?.forEach { acEl ->
                acEl.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.let { cpvSet.add(it) }
            }
        }

        // Deadline
        val tenderPeriod = data["tenderPeriod"]?.jsonObject
        val deadline = tenderPeriod?.get("endDate")?.jsonPrimitive?.contentOrNull ?: "-"

        val url = "https://prozorro.gov.ua/tender/$id"

        return Tender(
            id = id,
            tenderId = tenderID,
            title = title,
            description = description,
            status = status,
            dateModified = dateModified,
            procuringEntityName = entityName,
            procuringEntityEdrpou = edrpou,
            valueAmount = amount,
            valueCurrency = currency,
            cpvCodes = cpvSet.joinToString(", "),
            deadline = deadline,
            tenderUrl = url
        )
    }
}
