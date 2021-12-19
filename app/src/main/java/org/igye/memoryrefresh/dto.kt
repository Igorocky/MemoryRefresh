package org.igye.memoryrefresh

data class BeErr(val code:Int, val msg: String)
data class BeRespose<T>(val data: T? = null, val err: BeErr? = null) {
    fun <B> mapData(mapper:(T) -> B): BeRespose<B> = if (data != null) {
        BeRespose(data = mapper(data))
    } else {
        (this as BeRespose<B>)
    }
}
data class ListOfItems<T>(val complete: Boolean, val items: List<T>)

data class Tag(val id:Long, val createdAt:Long, val name:String)
data class Note(val id:Long, val createdAt:Long, val isDeleted:Boolean = false, val text:String, val tagIds: List<Long>)
data class Backup(val name: String, val size: Long)

enum class SharedFileType {
    BACKUP, KEYSTORE
}

data class HttpServerSettings(
    val keyStoreName: String = "", val keyStorePassword: String = "", val keyAlias: String = "", val privateKeyPassword: String = "",
    val port: Int = 8443, val serverPassword: String = "0000"
)
data class AppSettings(
    val httpServerSettings: HttpServerSettings
)

data class HttpServerState(val isRunning: Boolean, val url: String?, val settings: HttpServerSettings)