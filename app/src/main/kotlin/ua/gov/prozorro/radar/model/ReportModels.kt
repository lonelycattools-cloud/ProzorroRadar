package ua.gov.prozorro.radar.model

data class DashboardMetrics(
    val totalTenders: Int = 0,
    val newToday: Int = 0,
    val totalBudget: Double = 0.0,
    val activeTendersCount: Int = 0,
    val urgentDeadlinesCount: Int = 0,
    val lastScanTime: Long = 0L,
    val lastScanStatus: String = "Готовий до сканування",
    val nextScanEstimate: String = "За розкладом",
    val lastSyncDate: String = "Ще не встановлено"
)

data class ScanLog(
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val event: String,
    val count: Int = 0,
    val details: String = ""
)

sealed class ReportType(val title: String, val description: String) {
    object SuppliersCustomersByCpv : ReportType(
        "Замовники за категоріями CPV",
        "Аналіз замовників за обраними категоріями класифікатора за вказаний період"
    )
    object WonTendersByEdrpou : ReportType(
        "Тендери за кодом ЄДРПОУ",
        "Список та сумарний бюджет тендерів конкретної організації"
    )
    object CompanyParticipation : ReportType(
        "Активність замовника за період",
        "Динаміка оголошених торгів організації в обраному часовому діапазоні"
    )
    object TopProcuringEntities : ReportType(
        "Топ-10 найбільших замовників",
        "Організації з найбільшим сумарним бюджетом закупівель"
    )
    object TopCpvCategories : ReportType(
        "Топ категорій CPV",
        "Найбільш популярні категорії товарів та послуг за сумою та кількістю"
    )
    object UrgentDeadlines : ReportType(
        "Календар дедлайнів",
        "Тендери, за якими строк подання завершується в найближчі 24-48 годин"
    )
}

data class ReportRow(
    val title: String,
    val subtitle: String = "",
    val count: Int = 0,
    val totalAmount: Double = 0.0,
    val extra: String = ""
)

data class ReportResult(
    val type: ReportType,
    val generatedAt: Long = System.currentTimeMillis(),
    val period: String = "Весь час",
    val totalItems: Int = 0,
    val totalSum: Double = 0.0,
    val rows: List<ReportRow> = emptyList(),
    val tenders: List<Tender> = emptyList()
)
