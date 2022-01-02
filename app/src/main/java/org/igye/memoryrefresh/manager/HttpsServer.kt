package org.igye.memoryrefresh.manager

import android.content.Context
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.igye.memoryrefresh.ui.CustomAssetsPathHandler
import org.igye.memoryrefresh.LoggerImpl
import org.igye.memoryrefresh.common.Utils.createMethodMap
import java.io.File
import java.security.KeyStore
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.toCharArray

class HttpsServer(
    appContext: Context,
    keyStoreFile: File,
    keyAlias: String,
    privateKeyPassword: String,
    keyStorePassword: String,
    portNum: Int,
    serverPassword: String,
    javascriptInterface: List<Any>,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val logger = LoggerImpl("http-server")
    private val beMethods = createMethodMap(javascriptInterface) { !it.restrictAccessViaHttps }

    private val assetsPathHandler: CustomAssetsPathHandler = CustomAssetsPathHandler(
        appContext = appContext,
        feBeBridge = "js/http-fe-be-bridge.js"
    )

    private val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also {keyStore->
        keyStoreFile.inputStream().use { keyStore.load(it, keyStorePassword.toCharArray()) }
    }

    private val SESSION_ID_COOKIE_NAME = "sessionid"
    private val sessionId = AtomicReference<String>(null)

    private val environment = applicationEngineEnvironment {
        log = LoggerImpl("ktor-app")
        sslConnector(
            keyStore = keyStore,
            keyAlias = keyAlias,
            keyStorePassword = { keyStorePassword.toCharArray() },
            privateKeyPassword = { privateKeyPassword.toCharArray() }
        ) {
            port = portNum
            keyStorePath = keyStoreFile
        }
        module {
            routing {
                get("/{...}") {
                    authenticated(call) {
                        val path = call.request.path()
                        if ("/" == path || path.startsWith("/css/") || path.startsWith("/js/")) {
                            withContext(ioDispatcher) {
                                val response = assetsPathHandler.handle(if ("/" == path) "index.html" else path)!!
                                call.respondOutputStream(contentType = ContentType.parse(response.mimeType), status = HttpStatusCode.OK) {
                                    response.data.use { it.copyTo(this) }
                                }
                            }
                        } else {
                            logger.error("Path not found: $path")
                            call.respond(status = HttpStatusCode.NotFound, message = "Not found.")
                        }
                    }
                }
                post("/be/{funcName}") {
                    authenticated(call) {
                        val funcName = call.parameters["funcName"]
                        withContext(defaultDispatcher) {
                            val beMethod = beMethods.get(funcName)
                            if (beMethod == null) {
                                val msg = "backend method '$funcName' was not found"
                                logger.error(msg)
                                call.respond(status = HttpStatusCode.NotFound, message = msg)
                            } else {
                                call.respondText(contentType = ContentType.Application.Json, status = HttpStatusCode.OK) {
                                    beMethod.invoke(call.receiveText())
                                }
                            }
                        }
                    }
                }
                post("/login") {
                    val formParameters = call.receiveParameters()
                    val password: String? = formParameters["password"]?.toString()
                    if (password == serverPassword) {
                        sessionId.set(UUID.randomUUID().toString())
                        call.response.cookies.append(SESSION_ID_COOKIE_NAME, sessionId.get().toString())
                    }
                    call.respondRedirect("/", permanent = true)
                }
            }
        }
    }

    private val httpsServer = embeddedServer(Netty, environment).start(wait = false)

    fun stop() {
        httpsServer.stop(0,0)
    }

    private suspend fun authenticated(call: ApplicationCall, onAuthenticated: suspend () -> Unit) {
        if (sessionId.get() == null || sessionId.get() != call.request.cookies[SESSION_ID_COOKIE_NAME]) {
            withContext(ioDispatcher) {
                val response = assetsPathHandler.handle("https-server-auth.html")!!
                call.respondOutputStream(contentType = ContentType.parse(response.mimeType), status = HttpStatusCode.OK) {
                    response.data.use { it.copyTo(this) }
                }
            }
        } else {
            onAuthenticated()
        }
    }
}
