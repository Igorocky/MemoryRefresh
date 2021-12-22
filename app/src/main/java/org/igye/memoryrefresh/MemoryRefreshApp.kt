package org.igye.memoryrefresh

import android.app.Application

class MemoryRefreshApp: Application() {
    private val log = LoggerImpl("MemoryRefreshApp")
    val appContainer by lazy { AppContainer(context = applicationContext) }

    override fun onTerminate() {
        log.debug("Terminating.")
        appContainer.repositoryManager.close()
        super.onTerminate()
    }
}