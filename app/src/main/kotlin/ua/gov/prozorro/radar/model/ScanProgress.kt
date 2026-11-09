package ua.gov.prozorro.radar.model

data class ScanProgress(
    val examinedCount: Int = 0,
    val foundCount: Int = 0,
    val currentFeedDate: String = "",
    val statusText: String = ""
)
