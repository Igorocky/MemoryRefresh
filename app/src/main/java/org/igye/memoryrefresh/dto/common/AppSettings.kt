package org.igye.memoryrefresh.dto.common

data class AppSettings(
    val httpServerSettings: HttpServerSettings,
    val delayCoefs: List<String>? = null
)
