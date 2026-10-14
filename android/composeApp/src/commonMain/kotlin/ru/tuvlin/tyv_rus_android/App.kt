package ru.tuvlin.tyv_rus_android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.tuvlin.tyv_rus_android.ui.DetailsScreen
import ru.tuvlin.tyv_rus_android.ui.darker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val vm: DictionaryViewModel = viewModel { DictionaryViewModel() }
    val ui by vm.state.collectAsStateWithLifecycle()
    val homonymLabels = remember(ui.items) {
        buildMap {
            ui.items.groupBy { it.second }.values.filter { it.size > 1 }.forEach { articles ->
                articles.forEachIndexed { index, article ->
                    put(article.first, "Статья ${index + 1} из ${articles.size}")
                }
            }
        }
    }

    MaterialTheme {
        if (ui.selected != null) {
            // экран деталей
            key(ui.selected) {
                DetailsScreen(
                    title = ui.selectedWord,
                    html = ui.selectedHtml,
                    onBack = vm::onBack,
                    onOpenWord = vm::openByHead
                )
            }
            return@MaterialTheme
        }

        // экран списка
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            contentColor = Color.Red,
            topBar = { TopAppBar(title = { Text("Тыва-орус сөстүк") }) }
        ) { innerPadding ->

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = 16.dp, end = 16.dp
                    )
            ) {
                OutlinedTextField(
                    value = ui.query,
                    onValueChange = vm::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        cursorColor = Color.Green.darker(0.5f),
                        focusedBorderColor = Color.Green.darker(0.7f),
                        unfocusedBorderColor = Color.Green.darker(0.5f),
                    ),
                    placeholder = { Text("Дилээр/Искать…", style = MaterialTheme.typography.titleLarge) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) }
                )

                Spacer(Modifier.height(8.dp))
                if (ui.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                ui.error?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.onQueryChange(ui.query) }) { Text("Повторить") }
                }
                if (!ui.isLoading && ui.error == null && ui.items.isEmpty()) {
                    Text("Ничего не найдено")
                }

                val listState = rememberLazyListState(
                    initialFirstVisibleItemIndex = vm.listIndex,
                    initialFirstVisibleItemScrollOffset = vm.listOffset
                )

                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val halfScreen = maxHeight * 0.5f                         // половина экрана в Dp
                    val imeBottom = WindowInsets.ime.asPaddingValues()
                        .calculateBottomPadding()                              // высота клавы в Dp
                    val bottomPad = if (imeBottom > halfScreen) imeBottom else halfScreen

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(innerPadding)
                            .imePadding(),                                     // окно не перекрывается клавой
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = bottomPad                                  // 👈 ключ: max(клава, пол-экрана)
                        )
                    ) {
                        items(ui.items, key = { it.first }) { (i, w) ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        w,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                supportingContent = homonymLabels[i]?.let { label ->
                                    { Text(label) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.saveListPosition(
                                            listState.firstVisibleItemIndex,
                                            listState.firstVisibleItemScrollOffset
                                        )
                                        vm.onSelect(i)
                                    }
                                    .padding(top = 0.dp, bottom = 16.dp)
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

    }
}
