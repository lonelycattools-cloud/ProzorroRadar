package ua.gov.prozorro.radar.model

import kotlinx.serialization.Serializable

@Serializable
data class Tender(
    val id: String, // Prozorro ID / Hash
    val tenderId: String = "", // e.g. UA-2026-11-09-001234-a
    val title: String,
    val description: String = "",
    val status: String = "active.tendering",
    val dateModified: String = "",
    val procuringEntityName: String = "-",
    val procuringEntityEdrpou: String = "",
    val valueAmount: Double = 0.0,
    val valueCurrency: String = "UAH",
    val cpvCodes: String = "",
    val deadline: String = "-",
    val tenderUrl: String = "",
    val emailStatus: String = "Pending", // Pending, Sent, History, Failed
    val isFavorite: Boolean = false,
    val foundAtTimestamp: Long = System.currentTimeMillis()
) {
    val formattedBudget: String
        get() = String.format("%,.2f %s", valueAmount, valueCurrency)

    val displayStatus: String
        get() = when (status) {
            "active.tendering" -> "Подання пропозицій"
            "active.enquiries" -> "Період уточнень"
            "active.auction" -> "Аукціон"
            "active.qualification" -> "Кваліфікація"
            "active.awarded" -> "Розглянуто"
            "complete" -> "Завершено"
            "cancelled" -> "Скасовано"
            "unsuccessful" -> "Не відбувся"
            else -> status
        }
}
