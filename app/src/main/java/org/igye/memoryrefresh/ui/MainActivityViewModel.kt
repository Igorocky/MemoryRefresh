package org.igye.memoryrefresh.ui

import android.content.Context
import org.igye.memoryrefresh.manager.DataManager
import org.igye.memoryrefresh.manager.HttpsServerManager
import org.igye.memoryrefresh.manager.RepositoryManager
import org.igye.memoryrefresh.manager.SettingsManager
import java.util.concurrent.ExecutorService

class MainActivityViewModel(
    appContext: Context,
    settingsManager: SettingsManager,
    dataManager: DataManager,
    repositoryManager: RepositoryManager,
    httpsServerManager: HttpsServerManager,
    beThreadPool: ExecutorService,
): WebViewViewModel(
    appContext = appContext,
    javascriptInterface = listOf(dataManager, repositoryManager, httpsServerManager, settingsManager),
    rootReactComponent = "ViewSelector",
    beThreadPool = beThreadPool
)
