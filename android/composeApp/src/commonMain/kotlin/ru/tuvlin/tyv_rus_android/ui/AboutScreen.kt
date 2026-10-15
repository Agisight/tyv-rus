package ru.tuvlin.tyv_rus_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(onClose: () -> Unit, onOpenLink: (String) -> Unit) {
    Surface(Modifier.fillMaxSize().semantics { paneTitle = "О словаре" }, color = MaterialTheme.colorScheme.background) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(title = { Text("О словаре", fontWeight = FontWeight.SemiBold) },
                    actions = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Закрыть информацию") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
            }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Surface(shape = RoundedCornerShape(28.dp), color = Color.Transparent) {
                        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF154DAD), Color(0xFF087D83))))) {
                            Box(Modifier.size(180.dp).align(Alignment.TopEnd).offset(x = 65.dp, y = (-65).dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape))
                            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(16.dp)) {
                                    Text("Ө", Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                }
                                Text("Тыва-орус сөстүк", color = Color(0xFFD7F0FF), style = MaterialTheme.typography.labelLarge)
                                Text("Сөстүк", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Text("Слово за словом", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                Text("Тувинско-русский словарь, который всегда рядом.", color = Color(0xFFE2F1FF),
                                    style = MaterialTheme.typography.bodyLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Офлайн", "Без рекламы", "Без регистрации").forEach { label ->
                                        Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(20.dp)) {
                                            Text(label, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White,
                                                style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Statistic("16 627", "словарных статей", Modifier.weight(1f))
                        Statistic("2", "направления поиска", Modifier.weight(1f))
                    }
                }
                item {
                    InfoCard("Как пользоваться") {
                        Feature(Icons.Default.MenuBook, "Словарь", "Ищите тувинское слово или его русское значение. Статьи содержат переводы, пометы и примеры.")
                        Feature(Icons.Default.ChatBubbleOutline, "Спросить", "Напишите вопрос своими словами: найдём значение, перевод или примеры в словаре. Можно уточнить: «А примеры?».")
                        Feature(Icons.Default.Spellcheck, "Подсказки", "Если в запросе опечатка, предложим близкое написание. Неоднозначные варианты можно проверить в статьях.")
                        Feature(Icons.Default.Share, "Поделиться", "Отправляйте статью или ответ с заголовком и отдельными строками «Тыва / Русский».")
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.FavoriteBorder, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Слова — из словаря", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Переводы и примеры берутся из словарных статей. Режим «Спросить» разбирает запросы и не сочиняет новые переводы.",
                                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                item {
                    InfoCard("Конфиденциальность") {
                        Feature(Icons.Default.Lock, "Ваши запросы остаются у вас", "Приложение не собирает и не отправляет разработчику личные данные, поисковые запросы или историю диалога. Нет аккаунтов, рекламы и аналитики.")
                        Text("Диалог хранится в памяти во время работы приложения. Его можно очистить кнопкой корзины возле поля ввода. Локальную базу можно удалить вместе с приложением или через настройки Android.", style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Внешние сервисы", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Google Переводчик получает выбранный текст только после нажатия его кнопки. Для этого внешнего сервиса действуют правила конфиденциальности Google.", style = MaterialTheme.typography.bodyMedium)
                        Text("«Поделиться» передаёт выбранную статью или ответ в системное меню Android. Приложение и дальнейшее действие выбираете вы.", style = MaterialTheme.typography.bodyMedium)
                        Text("Политика конфиденциальности · 15 октября 2026 г.", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    InfoCard("На связи") {
                        Text("Tyva Developer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Предложения и замечания о словаре:", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { onOpenLink("mailto:agisight@gmail.com") }, contentPadding = PaddingValues(0.dp)) {
                            Text("agisight@gmail.com")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                        }
                    }
                }
                item {
                    Text("Сөстүк · Тыва-орус сөстүк", Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Statistic(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
            Icon(icon, null, Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
