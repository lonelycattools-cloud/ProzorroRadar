package ua.gov.prozorro.radar.ui.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.gov.prozorro.radar.export.ExcelExporter
import ua.gov.prozorro.radar.model.DashboardMetrics
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.ui.components.DateScanDialog
import ua.gov.prozorro.radar.ui.components.MetricCard
import ua.gov.prozorro.radar.ui.components.TenderCard
import ua.gov.prozorro.radar.ui.components.TenderDetailDialog
import ua.gov.prozorro.radar.ui.theme.EmeraldLight
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary
import ua.gov.prozorro.radar.ui.theme.StatusActive
import ua.gov.prozorro.radar.ui.theme.StatusInfo
import ua.gov.prozorro.radar.ui.theme.StatusUrgent
import ua.gov.prozorro.radar.ui.theme.StatusWarning

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    tenders: List<Tender>,
    isScanning: Boolean,
    currentCpvCodes: String,
    onScanNow: () -> Unit,
    onScanFromDate: (String, String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onNavigateToArchive: () -> Unit,
    onExportAll: () -> Unit
) {
    var selectedTender by remember { mutableStateOf<Tender?>(null) }
    var showDateScanDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection()
        }

        item {
            ScannerControlCard(
                isScanning = isScanning,
                metrics = metrics,
                onScanNow = onScanNow,
                onOpenDateScanDialog = { showDateScanDialog = true },
                onExportAll = onExportAll
            )
        }

        item {
            Text(
                text = "Ключові метрики",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            MetricsGrid(metrics = metrics)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Знайдені закупівлі",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToArchive) {
                    Text("Всі (${tenders.size}) →", color = EmeraldPrimary)
                }
            }
        }

        if (tenders.isEmpty()) {
            item {
                EmptyTendersBanner(isScanning = isScanning, onScanNow = onScanNow)
            }
        } else {
            items(tenders.take(8), key = { it.id }) { tender ->
                val isRecent = isScanning && (System.currentTimeMillis() - tender.foundAtTimestamp < 60000L)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            fadeInSpec = tween(durationMillis = 400),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                ) {
                    TenderCard(
                        tender = tender,
                        isNewArrival = isRecent,
                        onClick = { selectedTender = tender },
                        onToggleFavorite = { onToggleFavorite(tender.id, tender.isFavorite) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    selectedTender?.let { tender ->
        TenderDetailDialog(
            tender = tender,
            onDismiss = { selectedTender = null },
            onToggleFavorite = { onToggleFavorite(tender.id, tender.isFavorite) }
        )
    }

    if (showDateScanDialog) {
        DateScanDialog(
            currentCheckpointDate = metrics.lastSyncDate,
            initialCpvCodes = currentCpvCodes,
            onDismiss = { showDateScanDialog = false },
            onStartScan = { isoDate, selectedCpvs ->
                showDateScanDialog = false
                onScanFromDate(isoDate, selectedCpvs)
            }
        )
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Logo / Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EmeraldPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Prozorro Радар",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Автономний монітор закупівель",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScannerControlCard(
    isScanning: Boolean,
    metrics: DashboardMetrics,
    onScanNow: () -> Unit,
    onOpenDateScanDialog: () -> Unit,
    onExportAll: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isScanning) EmeraldLight else StatusActive)
                        .alpha(if (isScanning) alphaAnim else 1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Сканування стрічки Prozorro…" else "Сканер у черзі",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isScanning) EmeraldLight else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metrics.lastScanStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Checkpoint indicator bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Точка зупинки: ${metrics.lastSyncDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onScanNow,
                        enabled = !isScanning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Пошук…")
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Оновити зараз")
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenDateScanDialog,
                        enabled = !isScanning,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("З дати…", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                OutlinedButton(
                    onClick = onExportAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Експорт пулу в Excel (.xlsx)", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun MetricsGrid(metrics: DashboardMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Всього в базі",
                value = metrics.totalTenders.toString(),
                subValue = "+${metrics.newToday} сьогодні",
                icon = Icons.Default.Gavel,
                accentColor = EmeraldPrimary
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Нових за 24г",
                value = metrics.newToday.toString(),
                subValue = "Свіжі",
                icon = Icons.Default.Today,
                accentColor = StatusInfo
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val budgetFormatted = if (metrics.totalBudget >= 1_000_000) {
                String.format("%.1f млн ₴", metrics.totalBudget / 1_000_000)
            } else {
                String.format("%,.0f ₴", metrics.totalBudget)
            }
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Бюджет пулу",
                value = budgetFormatted,
                subValue = "Сума",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = StatusActive
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Термінові дедлайни",
                value = metrics.urgentDeadlinesCount.toString(),
                subValue = "< 48 год",
                icon = Icons.Default.Alarm,
                accentColor = StatusUrgent
            )
        }
    }
}

@Composable
private fun EmptyTendersBanner(isScanning: Boolean, onScanNow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "База тендерів порожня",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Натисніть кнопку нижче, щоб провести перший пошук та наповнити базу актуальними торгами.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onScanNow,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Розпочати перший пошук")
            }
        }
    }
}
