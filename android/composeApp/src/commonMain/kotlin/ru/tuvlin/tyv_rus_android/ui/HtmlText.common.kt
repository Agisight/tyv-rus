package ru.tuvlin.tyv_rus_android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    onRefClick: (String) -> Unit = {}
)
