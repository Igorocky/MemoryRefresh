package org.igye.taggednotes

import android.content.Context
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
