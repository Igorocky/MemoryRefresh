package org.igye.memoryrefresh.dto

data class HttpServerState(val isRunning: Boolean, val url: String?, val settings: HttpServerSettings)