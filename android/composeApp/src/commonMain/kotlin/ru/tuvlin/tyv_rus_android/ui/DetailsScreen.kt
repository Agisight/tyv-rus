package ru.tuvlin.tyv_rus_android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    title: String,
    html: String,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit = {}
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = inner.calculateTopPadding(),
                    bottom = inner.calculateBottomPadding()
                )
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    HtmlText(
                        html = html,
                        modifier = Modifier.fillMaxWidth(),
                        onRefClick = { word ->
                            // вызови вверх (через параметр onOpenWord) или напрямую VM
                            onOpenWord.invoke(word)
                        }
                    )
                }
            }
        }
    }
}
