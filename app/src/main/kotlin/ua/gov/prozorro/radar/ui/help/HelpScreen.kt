package ua.gov.prozorro.radar.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.gov.prozorro.radar.ui.theme.EmeraldPrimary

@Composable
fun HelpScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Довідка та Інструкції",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Посібник користувача системи Prozorro Радар",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            HelpSectionCard(
                title = "1. Як працює моніторинг Prozorro",
                icon = Icons.Default.Timeline,
                content = """
                    • Застосунок підключається безпосередньо до офіційного відкритого API публічних закупівель (public.api.openprocurement.org 2.5).
                    • Моніторинг відбувається у зворотному хронологічному порядку (найновіші оголошені закупівлі перевіряються першими).
                    • Алгоритм «Розумний стоп»: коли сканер зустрічає 5 раніше збережених тендерів підряд, сесія перевірки автоматично зупиняється. Це заощаджує інтернет-трафік та заряд акумулятора.
                    • Ви можете запустити сканування вручну кнопкою «Оновити зараз» або покластися на фоновий розклад.
                """.trimIndent(),
                initiallyExpanded = true
            )
        }

        item {
            HelpSectionCard(
                title = "2. Налаштування класифікатора CPV",
                icon = Icons.Default.Category,
                content = """
                    • CPV (Єдиний закупівельний словник ДК 021:2015) — це 8-значний цифровий код, за яким класифікується предмет закупівлі.
                    • Префіксний пошук: якщо вказати префікс «3023», система відбере як «30230000-0» (Комп'ютерне обладнання), так і «30237000-9» (Частини та аксесуари).
                    • Можна вказувати декілька кодів через кому, наприклад: 3023, 4500, 3360.
                    • Якщо поле CPV залишити порожнім — будуть відбиратися всі закупівлі без обмеження за категоріями.
                """.trimIndent()
            )
        }

        item {
            HelpSectionCard(
                title = "3. Фонова робота та налаштування батареї",
                icon = Icons.Default.BatteryChargingFull,
                content = """
                    • Щоб Android не вимикав сканування під час блокування екрана, обов'язково встановіть режим «Без обмежень» у налаштуваннях контролю активності застосунку.
                    • Xiaomi / Redmi / POCO (MIUI, HyperOS):
                      Налаштування → Додатки → Prozorro Радар → Автозапуск: Увімкнено; Контроль активності: Без обмежень.
                    • Samsung (One UI):
                      Налаштування → Програми → Prozorro Радар → Батарея → Не обмежено.
                    • Після перезавантаження пристрою система автоматично відновить графік моніторингу за допомогою модуля BootReceiver.
                """.trimIndent()
            )
        }

        item {
            HelpSectionCard(
                title = "4. Сповіщення та нічний режим",
                icon = Icons.Default.Notifications,
                content = """
                    • При знаходженні нових закупівель з'являється системне Push-сповіщення з назвою тендера, бюджетом та замовником.
                    • Режим «Не турбувати на ніч» активний з 22:00 до 08:00 (налаштовується). У цей період сповіщення надходять без звуку та вібрації, не турбуючи ваш відпочинок.
                    • Клік по сповіщенню одразу відкриває картку тендера в додатку.
                """.trimIndent()
            )
        }

        item {
            HelpSectionCard(
                title = "5. Налаштування Email розсилки",
                icon = Icons.Default.Email,
                content = """
                    • Застосунок підтримує відправку гарно зверстаних HTML-дайджестів на вашу пошту щойно з'являються нові закупівлі.
                    • Для відправки через Gmail необхідно увімкнути 2-етапну перевірку в акаунті Google та створити «Пароль додатка» (App Password).
                    • Стандартні параметри Gmail:
                      Сервер: smtp.gmail.com
                      Порт: 587 (з підтримкою STARTTLS)
                      Логін: ваша_пошта@gmail.com
                      Пароль: згенерований 16-значний пароль додатку Google.
                    • Використовуйте кнопку «Перевірити підключення SMTP» для тесту.
                """.trimIndent()
            )
        }

        item {
            HelpSectionCard(
                title = "6. Звіти та вивантаження в Excel",
                icon = Icons.Default.TableChart,
                content = """
                    • Всі відібрані торги зберігаються в локальній базі даних на вашому пристрої.
                    • Експорт формує стандартний файл Microsoft Excel (.xlsx) або CSV з українськими заголовками та числовими форматами бюджетів.
                    • Вкладка «Звіти» дозволяє в один клік сформувати рейтинги замовників, контрагентів за ЄДРПОУ та аналітику за категоріями товарів.
                    • Сформований файл можна відкрити в Excel, Google Таблицях або надіслати в Telegram, Viber чи поштою.
                """.trimIndent()
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpSectionCard(
    title: String,
    icon: ImageVector,
    content: String,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
