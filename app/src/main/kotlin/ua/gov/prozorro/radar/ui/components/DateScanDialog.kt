package ua.gov.prozorro.radar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.gov.prozorro.radar.data.CpvCatalog
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateScanDialog(
    currentCheckpointDate: String,
    initialCpvCodes: String,
    onDismiss: () -> Unit,
    onStartScan: (startDateIso: String, selectedCpvs: String) -> Unit
) {
    val context = LocalContext.current
    val catalog = remember { CpvCatalog.getInstance(context) }

    val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'00:00:00", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val now = Calendar.getInstance()

    // Precalculate preset dates
    val todayIso = sdfIso.format(now.time)
    val todayDisplay = sdfDisplay.format(now.time)

    val threeDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -3) }
    val threeDaysIso = sdfIso.format(threeDaysAgo.time)

    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val sevenDaysIso = sdfIso.format(sevenDaysAgo.time)

    val fourteenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -14) }
    val fourteenDaysIso = sdfIso.format(fourteenDaysAgo.time)

    val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
    val thirtyDaysIso = sdfIso.format(thirtyDaysAgo.time)

    val firstOfCurrentMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstOfMonthIso = sdfIso.format(firstOfCurrentMonth.time)

    var selectedPreset by remember { mutableStateOf(sevenDaysIso) }
    var customDateInput by remember { mutableStateOf(sdfDisplay.format(sevenDaysAgo.time)) }

    // Categories state
    var selectedCpvs by remember { mutableStateOf(initialCpvCodes) }
    var showCpvPicker by remember { mutableStateOf(false) }
    var resolvedCpvNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(selectedCpvs) {
        val codes = selectedCpvs.split(",")
            .map { it.trim().split("-")[0].trim() }
            .filter { it.isNotEmpty() }
        val map = mutableMapOf<String, String>()
        for (c in codes) {
            val name = catalog.getName(c)
            if (name != null) {
                map[c] = name
            }
        }
        resolvedCpvNames = map
    }

    val cpvCodesList = remember(selectedCpvs) {
        selectedCpvs.split(",")
            .map { it.trim().split("-")[0].trim() }
            .filter { it.isNotEmpty() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Сканування стрічки",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Закрити")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info Banner about current checkpoint
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Поточна точка зупинки:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentCheckpointDate,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Сканер завантажить торги від обраної дати до теперішнього часу та продовжить плановий моніторинг із нової точки.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section 1: Period Selection
                Column {
                    Text(
                        text = "1. Період початку пошуку:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        "Відсьогодні" to todayIso,
                        "3 дні тому" to threeDaysIso,
                        "7 днів тому" to sevenDaysIso,
                        "14 днів тому" to fourteenDaysIso,
                        "30 днів тому" to thirtyDaysIso,
                        "1-е число місяця" to firstOfMonthIso
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, iso) ->
                            FilterChip(
                                selected = selectedPreset == iso,
                                onClick = {
                                    selectedPreset = iso
                                    customDateInput = iso.take(10)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customDateInput,
                        onValueChange = {
                            customDateInput = it
                            selectedPreset = "${it.trim()}T00:00:00"
                        },
                        label = { Text("Точна дата початку (РРРР-ММ-ДД)") },
                        placeholder = { Text("2026-11-01") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Section 2: CPV Categories Selection
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Категорії CPV (${cpvCodesList.size}):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { showCpvPicker = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Змінити", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    if (cpvCodesList.isEmpty()) {
                        Text(
                            text = "Всі категорії (без фільтрації за CPV)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            cpvCodesList.take(4).forEach { code ->
                                val name = resolvedCpvNames[code] ?: "Завантаження…"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$code: $name",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (cpvCodesList.size > 4) {
                                Text(
                                    text = "+ ще ${cpvCodesList.size - 4} категорій…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showCpvPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Каталог ДК 021:2015 з пошуком", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalIso = if (selectedPreset.contains("T")) selectedPreset else "${customDateInput.trim()}T00:00:00"
                    onStartScan(finalIso, selectedCpvs)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Почати сканування", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )

    if (showCpvPicker) {
        val currentList = selectedCpvs.split(",")
            .map { it.trim().split("-")[0].trim() }
            .filter { it.isNotEmpty() }

        CpvPickerDialog(
            initialSelectedCodes = currentList,
            onDismiss = { showCpvPicker = false },
            onConfirm = { updatedCodes ->
                selectedCpvs = updatedCodes.joinToString(", ")
                showCpvPicker = false
            }
        )
    }
}
