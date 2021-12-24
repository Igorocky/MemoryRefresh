package org.igye.memoryrefresh.ui

import android.content.Context
import org.igye.memoryrefresh.manager.DataManager
import org.igye.memoryrefresh.manager.HttpsServerManager
import java.util.concurrent.ExecutorService

class MainActivityViewModel(
    appContext: Context,
    dataManager: DataManager,
    httpsServerManager: HttpsServerManager,
    beThreadPool: ExecutorService,
): WebViewViewModel(
    appContext = appContext,
    javascriptInterface = listOf(dataManager, httpsServerManager),
    rootReactComponent = "ViewSelector",
    beThreadPool = beThreadPool
)
