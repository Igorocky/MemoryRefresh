package org.igye.memoryrefresh.config

import android.app.Application
import org.igye.memoryrefresh.LoggerImpl

class MemoryRefreshApp: Application() {
    private val log = LoggerImpl("MemoryRefreshApp")
    val appContainer by lazy { AppContainer(context = applicationContext) }

    override fun onTerminate() {
        log.debug("Terminating.")
        appContainer.repositoryManager.close()
        super.onTerminate()
    }
}