package org.igye.memoryrefresh.dto.common

data class HttpServerSettings(
    val keyStoreName: String = "", val keyStorePassword: String = "", val keyAlias: String = "", val privateKeyPassword: String = "",
    val port: Int = 8443, val serverPassword: String = "0000"
)
