package ua.gov.prozorro.radar.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SystemUpdate
import ua.gov.prozorro.radar.BuildConfig
import ua.gov.prozorro.radar.data.CpvCatalog
import ua.gov.prozorro.radar.ui.components.CpvPickerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.network.EmailSender
import ua.gov.prozorro.radar.service.WorkScheduler
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    isCheckingUpdate: Boolean = false,
    onCheckUpdate: () -> Unit = {},
    onResetSyncOffset: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val emailSender = remember { EmailSender(context) }

    // State bindings
    var scanInterval by remember { mutableStateOf(preferences.scanIntervalMinutes) }
    var autoMonitoring by remember { mutableStateOf(preferences.autoUpdateEnabled) }
    var cpvCodes by remember { mutableStateOf(preferences.cpvCodes) }
    var searchStatus by remember { mutableStateOf(preferences.searchStatus) }
    var recipientEmails by remember { mutableStateOf(preferences.recipientEmails) }
    var pushEnabled by remember { mutableStateOf(preferences.pushEnabled) }
    var dndEnabled by remember { mutableStateOf(preferences.dndNightModeEnabled) }

    // App Update & GitHub settings
    var githubRepo by remember { mutableStateOf(preferences.githubRepo) }
    var forceUpdate by remember { mutableStateOf(preferences.forceUpdate) }
    var syncDateDisplay by remember { mutableStateOf(preferences.lastSyncDate) }

    // SMTP settings state
    var smtpHost by remember { mutableStateOf(preferences.smtpHost) }
    var smtpPort by remember { mutableStateOf(preferences.smtpPort.toString()) }
    var smtpUser by remember { mutableStateOf(preferences.smtpUser) }
    var smtpPass by remember { mutableStateOf(preferences.smtpPass) }
    var isTestingSmtp by remember { mutableStateOf(false) }

    // Feedback state
    var feedbackSenderEmail by remember { mutableStateOf(preferences.userContactEmail) }
    var feedbackType by remember { mutableStateOf("Побажання") }
    var feedbackMessage by remember { mutableStateOf("") }
    var isSendingFeedback by remember { mutableStateOf(false) }

    // CPV Catalog & Picker
    val cpvCatalog = remember { CpvCatalog.getInstance(context) }
    var showCpvPickerDialog by remember { mutableStateOf(false) }
    var resolvedCategoryNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(cpvCodes) {
        val codes = cpvCodes.split(",").map { it.trim().split("-")[0].trim() }.filter { it.isNotEmpty() }
        val map = mutableMapOf<String, String>()
        for (c in codes) {
            val name = cpvCatalog.getName(c)
            if (name != null) {
                map[c] = name
            }
        }
        resolvedCategoryNames = map
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Налаштування",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Керування розкладом, категоріями, сповіщеннями та поштою",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Scanning & Schedule
        item {
            SettingsCard(title = "Розклад та фонове сканування", icon = Icons.Default.Schedule) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Автоматичне сканування",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Періодичний пошук нових торгів у фоні",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoMonitoring,
                        onCheckedChange = {
                            autoMonitoring = it
                            preferences.autoUpdateEnabled = it
                            if (it) WorkScheduler.schedulePeriodicScan(context, forceUpdate = true)
                            else WorkScheduler.cancelPeriodicScan(context)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Інтервал сканування:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15 хв", 30 to "30 хв", 60 to "1 год", 180 to "3 год", 360 to "6 год", 720 to "12 год", 1440 to "24 год").forEach { (mins, label) ->
                        FilterChip(
                            selected = scanInterval == mins,
                            onClick = {
                                scanInterval = mins
                                preferences.scanIntervalMinutes = mins
                                if (autoMonitoring) WorkScheduler.schedulePeriodicScan(context, forceUpdate = true)
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Точка продовження (Offset):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = syncDateDisplay,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        onClick = {
                            onResetSyncOffset()
                            syncDateDisplay = "Скинуто"
                        }
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Скинути", color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Section 2: CPV Codes & Categories
        item {
            SettingsCard(title = "Категорії закупівель (CPV)", icon = Icons.Default.Category) {
                Text(
                    text = "Оберіть категорії з офіційного каталогу ДК 021:2015 за назвою або цифровим кодом:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Big Button to Open CPV Catalog with live search
                Button(
                    onClick = { showCpvPickerDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Каталог ДК 021:2015 з живим пошуком", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Selected Categories with full Ukrainian names
                val currentCodes = cpvCodes.split(",")
                    .map { it.trim().split("-")[0].trim() }
                    .filter { it.isNotEmpty() }

                if (currentCodes.isNotEmpty()) {
                    Text(
                        text = "Активні категорії для відбору (${currentCodes.size}):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentCodes.forEach { code ->
                            val humanName = resolvedCategoryNames[code] ?: "Завантаження опису..."
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = code,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = humanName,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = currentCodes.filter { it != code }
                                            val newStr = updated.joinToString(", ")
                                            cpvCodes = newStr
                                            preferences.cpvCodes = newStr
                                        },
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
                    Text(
                        text = "Усі категорії (без обмежень). Будуть відбиратися всі тендери Prozorro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fast Presets
                Text(
                    text = "Швидке додавання популярних категорій:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "18100000" to "Формений та робочий одяг",
                        "30230000" to "IT та комп'ютери",
                        "45000000" to "Будівництво та ремонт",
                        "33600000" to "Медикаменти та ліки",
                        "09130000" to "Паливо (дизель/бензин)",
                        "79710000" to "Охоронні послуги",
                        "30190000" to "Офісна техніка",
                        "15800000" to "Продукти харчування"
                    )
                    presets.forEach { (code, name) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ $code $name",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = EmeraldPrimary,
                                    modifier = Modifier.clickable {
                                        val cleanList = cpvCodes.split(",").map { it.trim().split("-")[0].trim() }.filter { it.isNotEmpty() }
                                        if (!cleanList.contains(code)) {
                                            val newCpv = (cleanList + code).joinToString(", ")
                                            cpvCodes = newCpv
                                            preferences.cpvCodes = newCpv
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // Manual text entry fallback
                Text(
                    text = "Або відредагуйте коди вручну (через кому):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cpvCodes,
                    onValueChange = {
                        cpvCodes = it
                        preferences.cpvCodes = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = EmeraldPrimary
                    ),
                    placeholder = { Text("18100000, 30230000, 45000000") }
                )
            }
        }

        // Section 3: Push Notifications & DND
        item {
            SettingsCard(title = "Push-сповіщення та Нічний режим", icon = Icons.Default.Notifications) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Отримувати Push-сповіщення",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = {
                            pushEnabled = it
                            preferences.pushEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Режим «Не турбувати на ніч»",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Беззвучні сповіщення з 22:00 до 08:00",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dndEnabled,
                        onCheckedChange = {
                            dndEnabled = it
                            preferences.dndNightModeEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }
            }
        }

        // Section 4: Email Dispatch & SMTP
        item {
            SettingsCard(title = "Розсилка тендерів по Email", icon = Icons.Default.Email) {
                Text(
                    text = "Адреси для отримання нових тендерів (через кому):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = recipientEmails,
                    onValueChange = {
                        recipientEmails = it
                        preferences.recipientEmails = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = EmeraldPrimary
                    ),
                    placeholder = { Text("mycompany@domain.ua, director@gmail.com") }
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Параметри вашого поштового сервера (SMTP):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = {
                            smtpHost = it
                            preferences.smtpHost = it
                        },
                        label = { Text("Сервер SMTP") },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = smtpPort,
                        onValueChange = {
                            smtpPort = it
                            preferences.smtpPort = it.toIntOrNull() ?: 587
                        },
                        label = { Text("Порт") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = smtpUser,
                    onValueChange = {
                        smtpUser = it
                        preferences.smtpUser = it
                    },
                    label = { Text("Логін (Email)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = smtpPass,
                    onValueChange = {
                        smtpPass = it
                        preferences.smtpPass = it
                    },
                    label = { Text("Пароль або пароль додатку") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isTestingSmtp = true
                            val res = emailSender.testSmtpConnection()
                            isTestingSmtp = false
                            if (res.isSuccess) {
                                Toast.makeText(context, res.getOrNull(), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Помилка: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isTestingSmtp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTestingSmtp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Перевірити підключення SMTP")
                }
            }
        }

        // Section 5: Permissions & Background Setup
        item {
            SettingsCard(title = "Фонова робота та дозволи", icon = Icons.Default.BatteryAlert) {
                Text(
                    text = "Для безперебійного сканування за розкладом додатку потрібен дозвіл на роботу без обмежень енергоспоживання.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { openBatteryOptimizationSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Налаштування оптимізації акумулятора", color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { openAppSystemSettings(context) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Усі дозволи застосунку", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Section 6: GitHub In-App Updates
        item {
            SettingsCard(title = "Оновлення додатку (GitHub)", icon = Icons.Default.SystemUpdate) {
                Text(
                    text = "Додаток автоматично перевіряє офіційні релізи на GitHub, сповіщає про нові версії та дозволяє оновитися в один клік.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Поточна версія:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = githubRepo,
                    onValueChange = {
                        githubRepo = it
                        preferences.githubRepo = it
                    },
                    label = { Text("GitHub репозиторій (owner/repo)") },
                    placeholder = { Text("lonelycattools-cloud/ProzorroRadar") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Примусове оновлення",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Блокувати роботу, якщо доступна новіша версія",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = forceUpdate,
                        onCheckedChange = {
                            forceUpdate = it
                            preferences.forceUpdate = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCheckUpdate,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Перевірити оновлення зараз", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 7: Feedback to LonelyCatTools
        item {
            SettingsCard(title = "Зворотний зв'язок та побажання", icon = Icons.Default.Feedback) {
                Text(
                    text = "Напишіть нам свої побажання щодо розвитку або повідомте про проблему. Ваше повідомлення надійде безпосередньо розробникам на lonelycattools@gmail.com, і ми відповімо на вашу пошту.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = feedbackSenderEmail,
                    onValueChange = {
                        feedbackSenderEmail = it
                        preferences.userContactEmail = it
                    },
                    label = { Text("Ваша електронна адреса для відповіді") },
                    placeholder = { Text("name@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Побажання", "Помилка (Баг)", "Запитання").forEach { type ->
                        FilterChip(
                            selected = feedbackType == type,
                            onClick = { feedbackType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = feedbackMessage,
                    onValueChange = { feedbackMessage = it },
                    label = { Text("Текст вашого повідомлення") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (feedbackSenderEmail.isBlank() || !feedbackSenderEmail.contains("@")) {
                            Toast.makeText(context, "Вкажіть коректний Email для відповіді", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (feedbackMessage.isBlank()) {
                            Toast.makeText(context, "Введіть текст звернення", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            isSendingFeedback = true
                            val result = emailSender.sendFeedbackDirect(
                                senderEmail = feedbackSenderEmail,
                                type = feedbackType,
                                message = feedbackMessage
                            )
                            isSendingFeedback = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Дякуємо! Повідомлення надіслано.", Toast.LENGTH_LONG).show()
                                feedbackMessage = ""
                            } else {
                                // Fallback to email client
                                emailSender.openEmailClientForFeedback(
                                    context = context,
                                    senderEmail = feedbackSenderEmail,
                                    type = feedbackType,
                                    message = feedbackMessage
                                )
                            }
                        }
                    },
                    enabled = !isSendingFeedback,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSendingFeedback) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Надіслати розробникам", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showCpvPickerDialog) {
        val currentList = cpvCodes.split(",").map { it.trim().split("-")[0].trim() }.filter { it.isNotEmpty() }
        CpvPickerDialog(
            initialSelectedCodes = currentList,
            onDismiss = { showCpvPickerDialog = false },
            onConfirm = { updatedCodes ->
                val newStr = updatedCodes.joinToString(", ")
                cpvCodes = newStr
                preferences.cpvCodes = newStr
                showCpvPickerDialog = false
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

fun openBatteryOptimizationSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        openAppSystemSettings(context)
    }
}

fun openAppSystemSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
