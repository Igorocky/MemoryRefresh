package org.igye.memoryrefresh.ui

import android.content.Context
import org.igye.memoryrefresh.manager.DataManager
import org.igye.memoryrefresh.manager.HttpsServerManager
import org.igye.memoryrefresh.manager.RepositoryManager
import java.util.concurrent.ExecutorService

class MainActivityViewModel(
    appContext: Context,
    dataManager: DataManager,
    repositoryManager: RepositoryManager,
    httpsServerManager: HttpsServerManager,
    beThreadPool: ExecutorService,
): WebViewViewModel(
    appContext = appContext,
    javascriptInterface = listOf(dataManager, repositoryManager, httpsServerManager),
    rootReactComponent = "ViewSelector",
    beThreadPool = beThreadPool
)
