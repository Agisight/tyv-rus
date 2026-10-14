package ru.tuvlin.tyv_rus_android

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform