package org.igye.memoryrefresh.dto.common

data class HttpServerState(val isRunning: Boolean, val url: String?, val settings: HttpServerSettings)