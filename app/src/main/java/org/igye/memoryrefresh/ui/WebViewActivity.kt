package org.igye.memoryrefresh.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.igye.memoryrefresh.LoggerImpl

abstract class WebViewActivity<T : WebViewViewModel> : AppCompatActivity() {
    protected val log = LoggerImpl(this.javaClass.simpleName)
    protected abstract val viewModel: T

    override fun onCreate(savedInstanceState: Bundle?) {
        log.debug("Starting")
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(viewModel.getWebView())
    }

    override fun onDestroy() {
        log.debug("Destroying")
        viewModel.detachWebView()
        super.onDestroy()
    }
}

