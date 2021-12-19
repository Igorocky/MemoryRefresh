package org.igye.taggednotes

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

class CustomAssetsPathHandler(
    private val appContext: Context,
    private val rootReactComponent: String = "ViewSelector",
    private val feBeBridge: String
) : WebViewAssetLoader.PathHandler {
    private val delegate = WebViewAssetLoader.AssetsPathHandler(appContext)
    override fun handle(path: String): WebResourceResponse? {
        val result = delegate.handle(path)
        if (path.endsWith("index.html") && result != null) {
            return WebResourceResponse(
                result.mimeType,
                result.encoding,
                ByteArrayInputStream(
                    String(result.data.readBytes(), Charsets.UTF_8)
                        .replaceFirst("js/mock-fe-be-bridge.js", feBeBridge)
                        .replaceFirst(
                            "const ROOT_REACT_COMPONENT = null",
                            "const ROOT_REACT_COMPONENT = $rootReactComponent"
                        )
                        .toByteArray()
                )
            )
        } else {
            return result
        }
    }
}