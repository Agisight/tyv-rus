package ru.tuvlin.tyv_rus_android

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.tuvlin.tyv_rus_android.ui.DetailsScreen

private val LightDictionaryColors = lightColorScheme(
    primary = Color(0xFF2358B8), onPrimary = Color.White,
    secondary = Color(0xFF177A78), secondaryContainer = Color(0xFFDDF1EE),
    onSecondaryContainer = Color(0xFF155C58),
    background = Color(0xFFF5F7FB), onBackground = Color(0xFF19283E),
    surface = Color.White, onSurface = Color(0xFF19283E),
    surfaceVariant = Color(0xFFEAF0F8), onSurfaceVariant = Color(0xFF58677C),
    outlineVariant = Color(0xFFE1E7F0)
)
private val DarkDictionaryColors = darkColorScheme(
    primary = Color(0xFFADC7FF), secondary = Color(0xFF8AD3C9),
    secondaryContainer = Color(0xFF214B48), onSecondaryContainer = Color(0xFFB6EEE6),
    background = Color(0xFF111A28), surface = Color(0xFF1B2738),
    onBackground = Color(0xFFE6EDF7), onSurface = Color(0xFFE6EDF7),
    surfaceVariant = Color(0xFF26354A), onSurfaceVariant = Color(0xFFB3C0D2),
    outlineVariant = Color(0xFF35465E)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val vm: DictionaryViewModel = viewModel { DictionaryViewModel() }
    val ui by vm.state.collectAsStateWithLifecycle()
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    var showAbout by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val chatState = rememberLazyListState()
    val homonyms = remember(ui.entries) { ui.entries.groupingBy { it.word }.eachCount() }
    LaunchedEffect(ui.query, ui.direction) { listState.scrollToItem(0) }
    LaunchedEffect(assistant.replies.size, assistant.isLoading) {
        if (assistant.replies.isNotEmpty()) chatState.animateScrollToItem(assistant.replies.lastIndex)
    }
    val submit: () -> Unit = {
        if (question.isNotBlank() && !assistant.isLoading) {
            vm.ask(question)
            question = ""
        }
    }
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkDictionaryColors else LightDictionaryColors) {
        DictionaryBackHandler(ui.selected != null) { vm.onBack() }
        if (ui.selected != null) {
            DetailsScreen(ui.selectedWord, ui.selectedHtml, vm::onBack, vm::openByHead)
            return@MaterialTheme
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(37.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("Ө", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Сөстүк", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Тувинско-русский словарь", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    actions = {
                        if (ui.mode == AppMode.ASK && assistant.replies.isNotEmpty()) {
                            IconButton(onClick = vm::clearConversation) { Icon(Icons.Default.DeleteOutline, "Очистить диалог") }
                        }
                        IconButton(onClick = { showAbout = true }) { Icon(Icons.Default.Info, "О словаре и конфиденциальности") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                Column(Modifier.imePadding()) {
                    if (ui.mode == AppMode.ASK) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = question, onValueChange = { question = it.take(300) },
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp),
                                    placeholder = { Text("Спросите о слове…") }, maxLines = 3,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { submit() }),
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                                )
                                FilledIconButton(onClick = submit, enabled = question.isNotBlank() && !assistant.isLoading, modifier = Modifier.padding(bottom = 4.dp).size(48.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Отправить вопрос")
                                }
                            }
                        }
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        NavigationBarItem(selected = ui.mode == AppMode.DICTIONARY, onClick = { vm.setMode(AppMode.DICTIONARY) },
                            icon = { Icon(Icons.Default.MenuBook, null) }, label = { Text("Словарь") })
                        NavigationBarItem(selected = ui.mode == AppMode.ASK, onClick = { vm.setMode(AppMode.ASK) },
                            icon = { Icon(Icons.Default.ChatBubbleOutline, null) }, label = { Text("Спросить") })
                    }
                }
            }
        ) { padding ->
            if (ui.mode == AppMode.DICTIONARY) {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if (ui.query.isEmpty() && ui.direction == SearchDirection.TUVAN) {
                        Surface(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.Transparent) {
                            Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF244F9E), Color(0xFF167F85)))).padding(20.dp)) {
                                Text("Слово за словом", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(5.dp))
                                Text("16 627 статей · без интернета", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDAE9FA))
                            }
                        }
                    }
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ui.direction == SearchDirection.TUVAN, onClick = { vm.setDirection(SearchDirection.TUVAN) }, label = { Text("Тыва → Русский") })
                        FilterChip(selected = ui.direction == SearchDirection.RUSSIAN, onClick = { vm.setDirection(SearchDirection.RUSSIAN) }, label = { Text("Русский → Тыва") })
                    }
                    OutlinedTextField(
                        value = ui.query, onValueChange = vm::onQueryChange,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
                        shape = RoundedCornerShape(18.dp), leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text(if (ui.direction == SearchDirection.TUVAN) "Найдите тувинское слово" else "Например, медведь") },
                        trailingIcon = { if (ui.query.isNotEmpty()) IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Default.Close, "Очистить поиск") } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (ui.direction == SearchDirection.RUSSIAN) "Поиск по русским значениям" else "Слова и значения", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!ui.isLoading) Text(if (ui.direction == SearchDirection.RUSSIAN && ui.entries.size == 120) "Первые 120 статей" else "${ui.entries.size} статей", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (ui.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    ui.error?.let { error ->
                        Text(error, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.onQueryChange(ui.query) }) { Text("Повторить") }
                    }
                    if (!ui.isLoading && ui.error == null && ui.entries.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(12.dp))
                            Text(if (ui.query.isBlank()) "Введите русское слово" else "Пока ничего не найдено", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(if (ui.query.isBlank()) "Найдём тувинские слова по их переводу." else "Попробуйте начальную форму или спросите о слове.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { vm.setMode(AppMode.ASK); question = ui.query }) { Text("Спросить о слове") }
                        }
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(ui.entries, key = { it.id }) { entry ->
                            EntryCard(entry, homonyms[entry.word]?.takeIf { it > 1 }?.let { "Несколько значений" }) {
                                vm.saveListPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                                vm.onSelect(entry.id)
                            }
                        }
                    }
                }
            } else {
                if (assistant.replies.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(66.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Спросите о слове", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Найдём значение, перевод или пример в словаре.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(22.dp))
                        listOf("Как по-тувински медведь?", "Что означает таваар?", "Покажи примеры со словом таваар").forEach { sample ->
                            OutlinedButton(onClick = { vm.ask(sample) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Text(sample, Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Ответы из словаря · работает офлайн", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(state = chatState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        items(assistant.replies, key = { it.id }) { reply ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(modifier = Modifier.align(Alignment.End).widthIn(max = 320.dp), color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp)) {
                                    Text(reply.question, Modifier.padding(15.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Icon(Icons.Default.MenuBook, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Text("Сөстүк · по словарю", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (reply.answer.isEmpty()) {
                                    LinearProgressIndicator(Modifier.width(120.dp))
                                } else {
                                    Text(reply.answer, style = MaterialTheme.typography.bodyLarge)
                                    reply.examples.forEach { example ->
                                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
                                            Text(example, Modifier.fillMaxWidth().padding(14.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                    reply.entries.forEach { entry -> EntryCard(entry, "Открыть словарную статью") { vm.onSelect(entry.id) } }
                                    TextButton(onClick = {
                                        runCatching { uriHandler.openUri(googleTranslateUrl(reply.translationText, reply.russianQuery)) }.onFailure { linkError = true }
                                    }) {
                                        Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text("Открыть текст в Google Переводчике")
                                    }
                                    Text("Внешний онлайн-сервис получит выбранный текст по вашему нажатию.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showAbout) {
            AlertDialog(onDismissRequest = { showAbout = false }, title = { Text("О словаре") }, confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Понятно") } },
                text = {
                    Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Тыва-орус сөстүк", fontWeight = FontWeight.SemiBold)
                        Text("16 627 статей. Словарь и поиск по русским значениям работают без интернета.")
                        Text("Режим «Спросить» распознаёт типовые вопросы и показывает найденные статьи. Его ответы берутся из словаря.")
                        Text("Политика конфиденциальности", fontWeight = FontWeight.SemiBold)
                        Text("Приложение не собирает и не отправляет разработчику личные данные, поисковые запросы или историю диалога. В нём нет аккаунтов, рекламы и аналитики. Диалог хранится в памяти во время работы приложения и очищается кнопкой корзины. Локальную базу можно удалить вместе с приложением или через настройки Android.")
                        Text("Кнопка Google Переводчика открывает внешний сервис и передаёт ему выбранный текст только по вашему нажатию. Для этого сервиса действуют правила конфиденциальности Google.")
                        Text("Разработчик: Tyva Developer\nКонтакт: agisight@gmail.com\nПриложение: ru.tyv.sostuk\nДата: 15 октября 2026 г.", style = MaterialTheme.typography.bodySmall)
                    }
                })
        }
        if (linkError) AlertDialog(onDismissRequest = { linkError = false }, title = { Text("Не удалось открыть ссылку") }, text = { Text("Проверьте, установлен ли браузер. Словарь продолжает работать без интернета.") }, confirmButton = { TextButton(onClick = { linkError = false }) { Text("Понятно") } })
    }
}

@Composable
private fun EntryCard(entry: DictionaryEntry, label: String? = null, onClick: () -> Unit) {
    val swatches = listOf(Color(0xFF2358B8), Color(0xFF177A78), Color(0xFF8B6233), Color(0xFF6A57A2))
    val accent = swatches[(entry.word.hashCode() and Int.MAX_VALUE) % swatches.size]
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.13f), CircleShape), contentAlignment = Alignment.Center) {
                Text(entry.word.take(1).uppercase(), color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (entry.preview.isNotBlank()) Text(entry.preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                entry.examples.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                label?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp).align(Alignment.CenterVertically), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
