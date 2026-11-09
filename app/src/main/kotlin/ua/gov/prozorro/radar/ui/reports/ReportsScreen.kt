package ua.gov.prozorro.radar.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.gov.prozorro.radar.model.ReportResult
import ua.gov.prozorro.radar.model.ReportRow
import ua.gov.prozorro.radar.model.ReportType
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary
import ua.gov.prozorro.radar.ui.theme.StatusActive
import ua.gov.prozorro.radar.ui.theme.StatusInfo

@Composable
fun ReportsScreen(
    tenders: List<Tender>,
    topEntities: List<ReportRow>,
    topCpvs: List<ReportRow>,
    onExportReport: (ReportResult) -> Unit
) {
    var selectedReportType by remember { mutableStateOf<ReportType>(ReportType.TopProcuringEntities) }
    var selectedPeriodDays by remember { mutableStateOf(30) } // 7, 30, 90, 36500 (all)
    var filterInput by remember { mutableStateOf("") } // CPV prefix or EDRPOU

    // Generate dynamic report result based on selection
    val reportResult = remember(selectedReportType, selectedPeriodDays, filterInput, tenders, topEntities, topCpvs) {
        val now = System.currentTimeMillis()
        val minTime = if (selectedPeriodDays > 365) 0L else now - (selectedPeriodDays * 24 * 60 * 60 * 1000L)
        val periodTenders = tenders.filter { it.foundAtTimestamp >= minTime }

        val periodLabel = when (selectedPeriodDays) {
            7 -> "За останні 7 днів"
            30 -> "За останні 30 днів"
            90 -> "За останні 90 днів"
            else -> "За весь час"
        }

        when (selectedReportType) {
            is ReportType.TopProcuringEntities -> {
                val grouped = periodTenders.groupBy { it.procuringEntityName }
                    .map { (name, list) ->
                        val edrpou = list.firstOrNull()?.procuringEntityEdrpou ?: ""
                        ReportRow(
                            title = name,
                            subtitle = if (edrpou.isNotEmpty()) "ЄДРПОУ: $edrpou" else "Замовник",
                            count = list.size,
                            totalAmount = list.sumOf { it.valueAmount },
                            extra = "${list.size} тенд."
                        )
                    }
                    .sortedByDescending { it.totalAmount }
                    .take(15)

                ReportResult(
                    type = selectedReportType,
                    period = periodLabel,
                    totalItems = grouped.sumOf { it.count },
                    totalSum = grouped.sumOf { it.totalAmount },
                    rows = grouped
                )
            }

            is ReportType.SuppliersCustomersByCpv -> {
                val cpvQuery = filterInput.trim()
                val matched = periodTenders.filter {
                    if (cpvQuery.isBlank()) true else it.cpvCodes.contains(cpvQuery, ignoreCase = true)
                }

                val grouped = matched.groupBy { it.procuringEntityName }
                    .map { (entity, list) ->
                        ReportRow(
                            title = entity,
                            subtitle = "CPV: " + list.flatMap { it.cpvCodes.split(",") }.map { it.trim() }.distinct().take(2).joinToString(", "),
                            count = list.size,
                            totalAmount = list.sumOf { it.valueAmount },
                            extra = "${list.size} закупівель"
                        )
                    }
                    .sortedByDescending { it.totalAmount }

                ReportResult(
                    type = selectedReportType,
                    period = periodLabel,
                    totalItems = matched.size,
                    totalSum = matched.sumOf { it.valueAmount },
                    rows = grouped
                )
            }

            is ReportType.WonTendersByEdrpou -> {
                val edrpouQuery = filterInput.trim()
                val matched = periodTenders.filter {
                    if (edrpouQuery.isBlank()) it.procuringEntityEdrpou.isNotEmpty()
                    else it.procuringEntityEdrpou.contains(edrpouQuery)
                }

                val grouped = matched.groupBy { it.procuringEntityEdrpou }
                    .map { (edrpou, list) ->
                        val orgName = list.firstOrNull()?.procuringEntityName ?: "Організація"
                        ReportRow(
                            title = orgName,
                            subtitle = "ЄДРПОУ: $edrpou",
                            count = list.size,
                            totalAmount = list.sumOf { it.valueAmount },
                            extra = "${list.size} торгів"
                        )
                    }
                    .sortedByDescending { it.totalAmount }

                ReportResult(
                    type = selectedReportType,
                    period = periodLabel,
                    totalItems = matched.size,
                    totalSum = matched.sumOf { it.valueAmount },
                    rows = grouped
                )
            }

            is ReportType.TopCpvCategories -> {
                val cpvMap = mutableMapOf<String, MutableList<Tender>>()
                for (t in periodTenders) {
                    val codes = t.cpvCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    for (c in codes) {
                        val prefix = if (c.length >= 4) c.take(4) else c
                        cpvMap.getOrPut(prefix) { mutableListOf() }.add(t)
                    }
                }

                val rows = cpvMap.map { (prefix, list) ->
                    val distinctTenders = list.distinctBy { it.id }
                    ReportRow(
                        title = "Категорія ДК 021:2015: $prefix****",
                        subtitle = "${distinctTenders.size} закупівель за категорією",
                        count = distinctTenders.size,
                        totalAmount = distinctTenders.sumOf { it.valueAmount },
                        extra = String.format("%,.0f грн", distinctTenders.sumOf { it.valueAmount })
                    )
                }.sortedByDescending { it.totalAmount }.take(15)

                ReportResult(
                    type = selectedReportType,
                    period = periodLabel,
                    totalItems = periodTenders.size,
                    totalSum = periodTenders.sumOf { it.valueAmount },
                    rows = rows
                )
            }

            is ReportType.UrgentDeadlines -> {
                val urgent = periodTenders.filter { it.deadline != "-" }
                    .sortedBy { it.deadline }

                val rows = urgent.map { t ->
                    ReportRow(
                        title = t.title,
                        subtitle = "${t.procuringEntityName} • Дедлайн: ${t.deadline.take(16)}",
                        count = 1,
                        totalAmount = t.valueAmount,
                        extra = t.formattedBudget
                    )
                }

                ReportResult(
                    type = selectedReportType,
                    period = periodLabel,
                    totalItems = urgent.size,
                    totalSum = urgent.sumOf { it.valueAmount },
                    rows = rows
                )
            }

            else -> {
                ReportResult(type = selectedReportType, period = periodLabel)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Аналітика та Звіти",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Аналітичні вибірки за категоріями, контрагентами та дедлайнами",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Report Types Horizontal Selector
            Text(
                text = "Оберіть звіт:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ReportTypeChip(
                        title = "Топ замовників",
                        icon = Icons.Default.Business,
                        selected = selectedReportType is ReportType.TopProcuringEntities,
                        onClick = { selectedReportType = ReportType.TopProcuringEntities }
                    )
                }
                item {
                    ReportTypeChip(
                        title = "Замовники за CPV",
                        icon = Icons.Default.Category,
                        selected = selectedReportType is ReportType.SuppliersCustomersByCpv,
                        onClick = { selectedReportType = ReportType.SuppliersCustomersByCpv }
                    )
                }
                item {
                    ReportTypeChip(
                        title = "Тендери за ЄДРПОУ",
                        icon = Icons.Default.Assessment,
                        selected = selectedReportType is ReportType.WonTendersByEdrpou,
                        onClick = { selectedReportType = ReportType.WonTendersByEdrpou }
                    )
                }
                item {
                    ReportTypeChip(
                        title = "Топ категорій CPV",
                        icon = Icons.Default.TrendingUp,
                        selected = selectedReportType is ReportType.TopCpvCategories,
                        onClick = { selectedReportType = ReportType.TopCpvCategories }
                    )
                }
                item {
                    ReportTypeChip(
                        title = "Дедлайни",
                        icon = Icons.Default.Schedule,
                        selected = selectedReportType is ReportType.UrgentDeadlines,
                        onClick = { selectedReportType = ReportType.UrgentDeadlines }
                    )
                }
            }
        }

        item {
            // Period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Період:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(7 to "7 дн", 30 to "30 дн", 90 to "90 дн", 10000 to "Весь час").forEach { (days, label) ->
                    FilterChip(
                        selected = selectedPeriodDays == days,
                        onClick = { selectedPeriodDays = days },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }
            }
        }

        // Conditional input for CPV or EDRPOU
        if (selectedReportType is ReportType.SuppliersCustomersByCpv || selectedReportType is ReportType.WonTendersByEdrpou) {
            item {
                val hint = if (selectedReportType is ReportType.SuppliersCustomersByCpv) {
                    "Введіть префікс CPV (напр. 3023 або 4500)"
                } else {
                    "Введіть код ЄДРПОУ (напр. 00037256)"
                }

                OutlinedTextField(
                    value = filterInput,
                    onValueChange = { filterInput = it },
                    label = { Text(hint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = EmeraldPrimary
                    ),
                    singleLine = true
                )
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = selectedReportType.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedReportType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Позицій у звіті", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = reportResult.rows.size.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column {
                            Text(text = "Сумарний бюджет", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format("%,.0f ₴", reportResult.totalSum),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onExportReport(reportResult) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Вивантажити звіт в Excel (.xlsx)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Table Rows
        if (reportResult.rows.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "За вказаними критеріями даних не знайдено.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(reportResult.rows) { row ->
                ReportRowCard(row = row)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReportTypeChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReportRowCard(row: ReportRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.subtitle.isNotEmpty()) {
                    Text(
                        text = row.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (row.totalAmount > 0) {
                    Text(
                        text = String.format("%,.0f ₴", row.totalAmount),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                }
                if (row.extra.isNotEmpty()) {
                    Text(
                        text = row.extra,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
