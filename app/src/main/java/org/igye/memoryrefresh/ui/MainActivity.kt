package org.igye.memoryrefresh.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import org.igye.memoryrefresh.config.MemoryRefreshApp

class MainActivity : WebViewActivity<MainActivityViewModel>() {
    override val viewModel: MainActivityViewModel by viewModels {
        (application as MemoryRefreshApp).appContainer.viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as MemoryRefreshApp).appContainer.repositoryManager.shareFile.set { shareFile(it) }
    }

    override fun onDestroy() {
        (application as MemoryRefreshApp).appContainer.repositoryManager.shareFile.set { null }
        super.onDestroy()
    }

    private fun shareFile(fileUri: Uri) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Send to"))
    }
}

