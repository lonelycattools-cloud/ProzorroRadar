package ua.gov.prozorro.radar.ui.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ua.gov.prozorro.radar.data.CpvCatalog
import ua.gov.prozorro.radar.ui.components.CpvPickerDialog
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupWizardDialog(
    initialCpvCodes: String,
    initialRecipientEmails: String,
    initialScanIntervalMinutes: Int,
    initialPushEnabled: Boolean,
    onDismiss: () -> Unit,
    onFinish: (
        cpvCodes: String,
        recipientEmails: String,
        scanIntervalMinutes: Int,
        pushEnabled: Boolean,
        initialScanDays: Int
    ) -> Unit
) {
    val context = LocalContext.current
    val cpvCatalog = remember { CpvCatalog.getInstance(context) }

    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: CPV codes
    val initialList = remember(initialCpvCodes) {
        initialCpvCodes.split(",")
            .map { it.trim().split("-")[0].trim() }
            .filter { it.isNotEmpty() }
    }
    var selectedCodes by remember { mutableStateOf(initialList.toSet()) }
    var showCpvPickerDialog by remember { mutableStateOf(false) }
    var resolvedNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(selectedCodes) {
        val map = mutableMapOf<String, String>()
        for (code in selectedCodes) {
            val name = cpvCatalog.getName(code)
            if (name != null) {
                map[code] = name
            }
        }
        resolvedNames = map
    }

    // Step 2: Email
    var recipientEmails by remember { mutableStateOf(initialRecipientEmails) }

    // Step 3: Intervals & scan depth
    var scanIntervalMinutes by remember { mutableIntStateOf(initialScanIntervalMinutes) }
    var initialScanDays by remember { mutableIntStateOf(7) }
    var pushEnabled by remember { mutableStateOf(initialPushEnabled) }

    val finishAction: () -> Unit = {
        val finalCpvCodes = selectedCodes.joinToString(", ")
        onFinish(
            finalCpvCodes,
            recipientEmails.trim(),
            scanIntervalMinutes,
            pushEnabled,
            initialScanDays
        )
    }

    val popularPresets = remember {
        listOf(
            "Будівництво (4500)" to "4500",
            "IT та оргтехніка (3023)" to "3023",
            "Медицина (3360)" to "3360",
            "Паливо (0913)" to "0913",
            "Одяг (1810)" to "1810",
            "Транспорт (6000)" to "6000",
            "Охорона (7971)" to "7971",
            "Харчування (1580)" to "1580"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Майстер налаштування",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Крок $currentStep з 4",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = EmeraldPrimary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрити",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { currentStep / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldPrimary,
                    trackColor = EmeraldPrimary.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable step content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> {
                            // Step 1: Категорії закупівель (CPV)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Категорії закупівель (CPV)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Оберіть категорії тендерів, які вас цікавлять:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Popular category presets
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Популярні галузі:",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        popularPresets.forEach { (presetTitle, code) ->
                                            val isSelected = selectedCodes.any { it == code || it.startsWith(code) }
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    val matches = selectedCodes.filter { it == code || it.startsWith(code) }
                                                    selectedCodes = if (matches.isNotEmpty()) {
                                                        selectedCodes - matches.toSet()
                                                    } else {
                                                        selectedCodes + code
                                                    }
                                                },
                                                label = { Text(presetTitle) },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                                    selectedLabelColor = EmeraldPrimary,
                                                    selectedLeadingIconColor = EmeraldPrimary
                                                )
                                            )
                                        }
                                    }
                                }

                                // Button to open full CPV catalog
                                OutlinedButton(
                                    onClick = { showCpvPickerDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Відкрити повний каталог ДК 021:2015",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Currently selected codes list
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Обрані категорії (${selectedCodes.size}):",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (selectedCodes.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            selectedCodes.forEach { code ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = code,
                                                                style = MaterialTheme.typography.labelMedium.copy(
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = EmeraldPrimary,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = resolvedNames[code] ?: "Завантаження...",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        IconButton(
                                                            onClick = { selectedCodes = selectedCodes - code },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Видалити",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Жодної категорії не обрано. Оберіть зі списку вище або відкрийте каталог.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Step 2: Email-розсилка
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Одержувачі сповіщень по Email",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Вкажіть ваші електронні адреси (через кому), на які надсилати нові знайдені торги:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedTextField(
                                    value = recipientEmails,
                                    onValueChange = { recipientEmails = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            text = "tender@company.ua, boss@gmail.com",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        if (recipientEmails.isNotEmpty()) {
                                            IconButton(onClick = { recipientEmails = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Очистити",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    minLines = 2,
                                    maxLines = 4
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Розсилка працює автоматично через системну поштову службу Prozorro Radar. Вводити SMTP паролі не потрібно!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Step 3: Розклад і глибина пошуку
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Періодичність та початкова глибина",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Налаштуйте частоту перевірки аукціонів та початковий період пошуку:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Section: Інтервал сканування у фоні
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Інтервал сканування у фоні:",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    val intervalOptions = listOf(
                                        15 to "15 хв",
                                        30 to "30 хв",
                                        60 to "1 год",
                                        120 to "2 год"
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        intervalOptions.forEach { (minutes, label) ->
                                            val isSelected = scanIntervalMinutes == minutes
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { scanIntervalMinutes = minutes },
                                                label = { Text(label) },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                                    selectedLabelColor = EmeraldPrimary,
                                                    selectedLeadingIconColor = EmeraldPrimary
                                                )
                                            )
                                        }
                                    }
                                }

                                // Section: Глибина первинного пошуку
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Глибина первинного пошуку:",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    val scanDayOptions = listOf(
                                        3 to "3 дні",
                                        7 to "7 днів",
                                        14 to "14 днів",
                                        30 to "30 днів"
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        scanDayOptions.forEach { (days, label) ->
                                            val isSelected = initialScanDays == days
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { initialScanDays = days },
                                                label = { Text(label) },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                                    selectedLabelColor = EmeraldPrimary,
                                                    selectedLeadingIconColor = EmeraldPrimary
                                                )
                                            )
                                        }
                                    }
                                }

                                // Section: Сповіщення на телефоні
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Сповіщення на телефоні:",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Notifications,
                                                        contentDescription = null,
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Миттєві push-повідомлення про нові торги",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Switch(
                                                checked = pushEnabled,
                                                onCheckedChange = { pushEnabled = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = EmeraldPrimary,
                                                    checkedTrackColor = EmeraldPrimary.copy(alpha = 0.35f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // Step 4: Підсумок та старт
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Готово до запуску!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Перевірте обрані параметри моніторингу:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // CPV Categories
                                        WizardSummaryRow(
                                            icon = Icons.Default.Category,
                                            title = "Категорії CPV",
                                            value = if (selectedCodes.isEmpty()) {
                                                "Не обрано (буде збережено порожнім)"
                                            } else {
                                                "${selectedCodes.size} кат. (${selectedCodes.joinToString(", ")})"
                                            }
                                        )

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )

                                        // Email
                                        WizardSummaryRow(
                                            icon = Icons.Default.Email,
                                            title = "Email для звітів",
                                            value = if (recipientEmails.isBlank()) {
                                                "Не вказано (лише в додатку)"
                                            } else {
                                                recipientEmails.trim()
                                            }
                                        )

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )

                                        // Interval
                                        WizardSummaryRow(
                                            icon = Icons.Default.Schedule,
                                            title = "Інтервал сканування",
                                            value = "кожні $scanIntervalMinutes хв"
                                        )

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )

                                        // Depth
                                        WizardSummaryRow(
                                            icon = Icons.Default.History,
                                            title = "Первинний збір",
                                            value = "за $initialScanDays днів"
                                        )

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )

                                        // Push
                                        WizardSummaryRow(
                                            icon = Icons.Default.Notifications,
                                            title = "Push-сповіщення",
                                            value = if (pushEnabled) "Увімкнено" else "Вимкнено",
                                            valueColor = if (pushEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Big primary start button
                                Button(
                                    onClick = finishAction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Зберегти та почати пошук тендерів!",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Navigation footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        TextButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Назад")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (currentStep < 4) {
                        Button(
                            onClick = { currentStep++ },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Далі", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = finishAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Почати роботу", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCpvPickerDialog) {
        CpvPickerDialog(
            initialSelectedCodes = selectedCodes.toList(),
            onDismiss = { showCpvPickerDialog = false },
            onConfirm = { updatedCodes ->
                selectedCodes = updatedCodes
                    .map { it.trim().split("-")[0].trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                showCpvPickerDialog = false
            }
        )
    }
}

@Composable
private fun WizardSummaryRow(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = valueColor
            )
        }
    }
}
