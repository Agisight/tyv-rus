package ru.tuvlin.tyv_rus_android

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@androidx.compose.runtime.Composable
expect fun DictionaryBackHandler(enabled: Boolean, onBack: () -> Unit)
