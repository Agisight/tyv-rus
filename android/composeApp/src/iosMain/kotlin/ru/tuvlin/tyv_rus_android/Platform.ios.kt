package ru.tuvlin.tyv_rus_android

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@androidx.compose.runtime.Composable
actual fun DictionaryBackHandler(enabled: Boolean, onBack: () -> Unit) {}
