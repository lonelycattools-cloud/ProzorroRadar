package ua.gov.prozorro.radar.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import ua.gov.prozorro.radar.data.CpvCatalog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.gov.prozorro.radar.model.Tender
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary
import ua.gov.prozorro.radar.ui.theme.SlateBorder
import ua.gov.prozorro.radar.ui.theme.SlateSurface
import ua.gov.prozorro.radar.ui.theme.SlateSurfaceVariant
import ua.gov.prozorro.radar.ui.theme.StatusActive
import ua.gov.prozorro.radar.ui.theme.StatusUrgent
import ua.gov.prozorro.radar.ui.theme.StatusWarning

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String = "",
    icon: ImageVector,
    accentColor: Color = EmeraldPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (subValue.isNotEmpty()) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TenderCard(
    tender: Tender,
    isNewArrival: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current

    val pulseAnim = if (isNewArrival) {
        val infiniteTransition = rememberInfiniteTransition(label = "tenderPulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "borderPulse"
        )
        alpha
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNewArrival) EmeraldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isNewArrival) 1.5.dp else 1.dp,
            if (isNewArrival) EmeraldPrimary.copy(alpha = pulseAnim) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Status badge & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = tender.status, displayStatus = tender.displayStatus)
                    if (isNewArrival) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldPrimary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "НОВИЙ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tender.valueAmount > 0) {
                        Text(
                            text = tender.formattedBudget,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (tender.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Обране",
                            tint = if (tender.isFavorite) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = tender.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Procuring Entity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tender.procuringEntityName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (tender.procuringEntityEdrpou.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = tender.procuringEntityEdrpou,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Deadline & CPV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tender.deadline != "-") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = StatusUrgent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tender.deadline.take(16).replace("T", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusUrgent
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Quick buttons
                Row {
                    IconButton(
                        onClick = { shareTender(context, tender) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поділитися",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { openProzorroUrl(context, tender.tenderUrl) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Відкрити",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // CPV Chips with human-readable Ukrainian name
            if (tender.cpvCodes.isNotEmpty()) {
                val catalog = remember { CpvCatalog.getInstance(context) }
                val cpvList = remember(tender.cpvCodes) {
                    tender.cpvCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
                var resolvedName by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(tender.cpvCodes) {
                    if (cpvList.isNotEmpty()) {
                        resolvedName = catalog.getName(cpvList.first())
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val firstCpv = cpvList.firstOrNull() ?: ""
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = if (resolvedName != null) "$firstCpv • $resolvedName" else firstCpv,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (cpvList.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "+ ще ${cpvList.size - 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, displayStatus: String) {
    val (bgColor, textColor) = when (status) {
        "active.tendering" -> StatusActive.copy(alpha = 0.15f) to StatusActive
        "active.auction" -> StatusWarning.copy(alpha = 0.15f) to StatusWarning
        "complete" -> Color(0xFF8B5CF6).copy(alpha = 0.15f) to Color(0xFF8B5CF6)
        "cancelled", "unsuccessful" -> StatusUrgent.copy(alpha = 0.15f) to StatusUrgent
        else -> SlateSurfaceVariant to Color(0xFF94A3B8)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = displayStatus,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TenderDetailDialog(
    tender: Tender,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val catalog = remember { CpvCatalog.getInstance(context) }
    var resolvedCpvText by remember { mutableStateOf(tender.cpvCodes) }

    LaunchedEffect(tender.cpvCodes) {
        val list = tender.cpvCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val resolved = list.map { code ->
            val n = catalog.getName(code)
            if (n != null) "$code\n• $n" else code
        }
        if (resolved.isNotEmpty()) {
            resolvedCpvText = resolved.joinToString("\n\n")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tender.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                DetailItem(label = "Очікувана вартість", value = tender.formattedBudget, highlight = true)
                DetailItem(label = "Замовник", value = tender.procuringEntityName)
                if (tender.procuringEntityEdrpou.isNotEmpty()) {
                    DetailItem(label = "Код ЄДРПОУ", value = tender.procuringEntityEdrpou)
                }
                DetailItem(label = "Статус", value = tender.displayStatus)
                DetailItem(label = "Категорія ДК 021:2015 (CPV)", value = resolvedCpvText)
                DetailItem(label = "Кінцевий строк", value = tender.deadline)
                DetailItem(label = "ID закупівлі", value = tender.tenderId.ifEmpty { tender.id })
                if (tender.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Опис:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tender.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { openProzorroUrl(context, tender.tenderUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("На Prozorro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String, highlight: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                color = if (highlight) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

fun openProzorroUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun shareTender(context: Context, tender: Tender) {
    val text = """
        Тендер Prozorro:
        ${tender.title}
        Бюджет: ${tender.formattedBudget}
        Замовник: ${tender.procuringEntityName}
        Дедлайн: ${tender.deadline}
        Посилання: ${tender.tenderUrl}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, tender.title)
        putExtra(Intent.EXTRA_TEXT, text)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(Intent.createChooser(intent, "Поділитися тендером").apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}
