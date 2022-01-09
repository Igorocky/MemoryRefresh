package org.igye.memoryrefresh.dto.common

data class AppSettings(
    val httpServerSettings: HttpServerSettings,
    val delayCoefs: List<String> = listOf("x1.2","x1.5","x2","x3")
)
