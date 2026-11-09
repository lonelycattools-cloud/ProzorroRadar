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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateScanDialog(
    currentCheckpointDate: String,
    onDismiss: () -> Unit,
    onStartScan: (String) -> Unit // returns ISO string e.g. "2026-11-01T00:00:00"
) {
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
                        text = "Сканувати з дати",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Закрити")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            text = "Фонове сканування за розкладом продовжує роботу саме з цієї точки. Якщо обрати дату в минулому, сканер вивантажить усі торги від цієї дати до теперішнього моменту і збереже новий чекпоїнт.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Оберіть період початку:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Presets
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
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom Date Input field
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalIso = if (selectedPreset.contains("T")) selectedPreset else "${customDateInput.trim()}T00:00:00"
                    onStartScan(finalIso)
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
}
