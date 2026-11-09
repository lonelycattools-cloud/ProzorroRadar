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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.gov.prozorro.radar.data.CpvCatalog
import ua.gov.prozorro.radar.model.ReportResult
import ua.gov.prozorro.radar.model.ReportRow
import ua.gov.prozorro.radar.model.ReportType
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.ui.components.CpvPickerDialog
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary
import ua.gov.prozorro.radar.util.CpvMatcher

@Composable
fun ReportsScreen(
    tenders: List<Tender>,
    topEntities: List<ReportRow>,
    topCpvs: List<ReportRow>,
    onExportReport: (ReportResult) -> Unit
) {
    val context = LocalContext.current
    val cpvCatalog = remember { CpvCatalog.getInstance(context) }

    var selectedReportType by remember { mutableStateOf<ReportType>(ReportType.TopProcuringEntities) }
    var selectedPeriodDays by remember { mutableStateOf(30) } // 7, 30, 90, 10000 (all)
    var filterInput by remember { mutableStateOf("") }
    var showCpvPickerDialog by remember { mutableStateOf(false) }

    // Resolved CPV human names for the top CPV categories
    var resolvedCpvNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        cpvCatalog.ensureLoaded()
    }

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
                    .take(20)

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
                // Use CpvMatcher so parent query (e.g. 18100000 or 1810000) matches all children (18141000, 18110000, etc.)
                val matched = periodTenders.filter {
                    if (cpvQuery.isBlank()) true else CpvMatcher.matches(it.cpvCodes, cpvQuery)
                }

                val grouped = matched.groupBy { it.procuringEntityName }
                    .map { (entity, list) ->
                        val edrpou = list.firstOrNull()?.procuringEntityEdrpou ?: ""
                        val cpvsSample = list.flatMap { it.cpvCodes.split(",") }
                            .map { it.trim().split("-")[0].trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .take(2)
                            .joinToString(", ")

                        ReportRow(
                            title = entity,
                            subtitle = if (edrpou.isNotEmpty()) "ЄДРПОУ: $edrpou • CPV: $cpvsSample" else "CPV: $cpvsSample",
                            count = list.size,
                            totalAmount = list.sumOf { it.valueAmount },
                            extra = "${list.size} торгів"
                        )
                    }
                    .sortedByDescending { it.totalAmount }
                    .take(30)

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
                    .take(30)

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
                    val codes = t.cpvCodes.split(",")
                        .map { it.trim().split("-")[0].trim() }
                        .filter { it.isNotEmpty() }
                    for (c in codes) {
                        val prefix = CpvMatcher.extractPrefix(c)
                        cpvMap.getOrPut(prefix) { mutableListOf() }.add(t)
                    }
                }

                val rows = cpvMap.map { (prefix, list) ->
                    val distinctTenders = list.distinctBy { it.id }
                    val name = resolvedCpvNames[prefix] ?: "Категорія ДК 021:2015 ($prefix)"
                    ReportRow(
                        title = "$prefix****: $name",
                        subtitle = "${distinctTenders.size} активних торгів у вибірці",
                        count = distinctTenders.size,
                        totalAmount = distinctTenders.sumOf { it.valueAmount },
                        extra = String.format("%,.0f ₴", distinctTenders.sumOf { it.valueAmount })
                    )
                }.sortedByDescending { it.totalAmount }.take(20)

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
                    .take(30)

                val rows = urgent.map { t ->
                    ReportRow(
                        title = t.title,
                        subtitle = "${t.procuringEntityName} • Дедлайн: ${t.deadline.take(16).replace("T", " ")}",
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
        // 1. Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Аналітика та Звіти",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Аналітичні вибірки за контрагентами, категоріями ДК 021 та дедлайнами",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2. Report Type Selector (Horizontal scrollable chips with clear padding)
        item {
            Text(
                text = "Оберіть тип аналітичного звіту:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            val reportTypes = listOf(
                ReportType.TopProcuringEntities to ("Топ замовників" to Icons.Default.Business),
                ReportType.SuppliersCustomersByCpv to ("Замовники за CPV" to Icons.Default.Category),
                ReportType.WonTendersByEdrpou to ("За ЄДРПОУ" to Icons.Default.Assessment),
                ReportType.TopCpvCategories to ("Рейтинг CPV" to Icons.Default.TrendingUp),
                ReportType.UrgentDeadlines to ("Дедлайни" to Icons.Default.Schedule)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(reportTypes) { (type, info) ->
                    val (title, icon) = info
                    val isSelected = selectedReportType::class == type::class
                    ReportTypeCard(
                        title = title,
                        icon = icon,
                        selected = isSelected,
                        onClick = {
                            selectedReportType = type
                            if (type !is ReportType.SuppliersCustomersByCpv && type !is ReportType.WonTendersByEdrpou) {
                                filterInput = ""
                            }
                        }
                    )
                }
            }
        }

        // 3. High-Contrast Period Selector (Zero vertical text! Equal-width buttons)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Період аналізу:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val periods = listOf(
                            7 to "7 днів",
                            30 to "30 днів",
                            90 to "90 днів",
                            10000 to "Весь час"
                        )
                        periods.forEach { (days, label) ->
                            val isSelected = selectedPeriodDays == days
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) EmeraldPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPeriodDays = days }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Filter Input with Quick Catalog Picker for CPV or EDRPOU
        if (selectedReportType is ReportType.SuppliersCustomersByCpv || selectedReportType is ReportType.WonTendersByEdrpou) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        val isCpv = selectedReportType is ReportType.SuppliersCustomersByCpv
                        Text(
                            text = if (isCpv) "Фільтр за кодом CPV (включає всі дочірні підкатегорії):" else "Фільтр за ЄДРПОУ замовника:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = filterInput,
                                onValueChange = { filterInput = it },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = EmeraldPrimary
                                ),
                                placeholder = {
                                    Text(if (isCpv) "1810, 3023, 0913..." else "00037256...")
                                },
                                singleLine = true,
                                trailingIcon = {
                                    if (filterInput.isNotEmpty()) {
                                        IconButton(onClick = { filterInput = "" }) {
                                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Очистити", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            )

                            if (isCpv) {
                                Button(
                                    onClick = { showCpvPickerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Каталог", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Executive Summary Card with Big Excel Export Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reportResult.type.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = reportResult.period,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Позицій у звіті",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${reportResult.rows.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Сумарний бюджет",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format("%,.0f ₴", reportResult.totalSum),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                        Text(
                            text = "Вивантажити звіт в Excel (.xlsx)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Деталізовані позиції звіту (${reportResult.rows.size}):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 7. Results List
        if (reportResult.rows.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Немає даних для обраного періоду та фільтрів",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            itemsIndexed(reportResult.rows) { index, row ->
                ReportRowCard(index = index + 1, row = row)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // CPV Catalog Picker Dialog
    if (showCpvPickerDialog) {
        CpvPickerDialog(
            initialSelectedCodes = if (filterInput.isNotBlank()) listOf(filterInput) else emptyList(),
            onDismiss = { showCpvPickerDialog = false },
            onConfirm = { updatedCodes ->
                if (updatedCodes.isNotEmpty()) {
                    filterInput = updatedCodes.first()
                }
                showCpvPickerDialog = false
            }
        )
    }
}

@Composable
private fun ReportTypeCard(
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
            if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReportRowCard(index: Int, row: ReportRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge (#1, #2, #3)
            Surface(
                shape = CircleShape,
                color = when (index) {
                    1 -> Color(0xFFEAB308).copy(alpha = 0.2f)
                    2 -> Color(0xFF94A3B8).copy(alpha = 0.2f)
                    3 -> Color(0xFFD97706).copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (index) {
                            1 -> Color(0xFFEAB308)
                            2 -> Color(0xFF94A3B8)
                            3 -> Color(0xFFD97706)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (row.subtitle.isNotEmpty()) {
                    Text(
                        text = row.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = row.extra,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
