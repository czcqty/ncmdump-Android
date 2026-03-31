package com.ncmdump.android

import android.content.Context
import android.content.SharedPreferences
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * WebView JavaScript Interface bridge
 * Provides the same API as the original Wails Go backend
 * Called from React frontend via window.Android.*
 */
class WebViewBridge(
    private val activity: MainActivity,
    private val webView: WebView
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs: SharedPreferences = activity.getSharedPreferences("ncmdump_config", Context.MODE_PRIVATE)

    /**
     * Open Android file picker for .ncm files
     * Result is returned via window.onFilesSelected callback
     */
    @JavascriptInterface
    fun selectFiles() {
        activity.runOnUiThread {
            activity.launchFilePicker()
        }
    }

    /**
     * Open Android folder picker
     * Result is returned via window.onFolderSelected callback
     */
    @JavascriptInterface
    fun selectFolder() {
        activity.runOnUiThread {
            activity.launchFolderPicker()
        }
    }

    /**
     * Open folder picker and list .ncm files recursively
     * Result is returned via window.onFolderFilesSelected callback
     */
    @JavascriptInterface
    fun selectFilesFromFolder(ext: String) {
        activity.runOnUiThread {
            activity.launchFolderFilePicker(ext)
        }
    }

    /**
     * Process NCM files - decrypt and convert
     * Emits file-status-changed events via window.onFileStatusChanged
     */
    @JavascriptInterface
    fun processFiles(filesJson: String, savePath: String) {
        scope.launch {
            try {
                val filesArray = JSONArray(filesJson)
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    val filePath = fileObj.getString("Name")
                    val status = fileObj.getString("Status")

                    if (status == "pending") {
                        launch {
                            processFile(filePath, i, savePath)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Process a single NCM file
     * Port of: App.processFile in app.go
     */
    private suspend fun processFile(filePath: String, index: Int, savePath: String) {
        emitFileStatus(index, "processing")
        try {
            val ncm = NcmCrypt(filePath)
            try {
                val result = ncm.dump(savePath)
                if (result) {
                    ncm.fixMetadata(true)
                    emitFileStatus(index, "done")
                } else {
                    emitFileStatus(index, "error")
                }
            } finally {
                ncm.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emitFileStatus(index, "error")
        }
    }

    /**
     * Emit file status change event to WebView
     */
    private fun emitFileStatus(index: Int, status: String) {
        activity.runOnUiThread {
            webView.evaluateJavascript(
                "if(window.onFileStatusChanged){window.onFileStatusChanged($index, '$status')}",
                null
            )
        }
    }

    /**
     * Load saved configuration
     */
    @JavascriptInterface
    fun loadConfig(): String {
        val saveTo = prefs.getString("save_to", "original") ?: "original"
        val path = prefs.getString("path", "") ?: ""
        val config = JSONObject()
        config.put("save_to", saveTo)
        config.put("path", path)
        return config.toString()
    }

    /**
     * Save configuration
     */
    @JavascriptInterface
    fun saveConfig(configJson: String) {
        try {
            val config = JSONObject(configJson)
            prefs.edit()
                .putString("save_to", config.optString("save_to", "original"))
                .putString("path", config.optString("path", ""))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---- Callback methods called from MainActivity ----

    fun onFilesSelected(files: List<String>) {
        val jsonArray = JSONArray(files)
        activity.runOnUiThread {
            webView.evaluateJavascript(
                "if(window.onFilesSelected){window.onFilesSelected('${jsonArray.toString().replace("'", "\\'")}')}",
                null
            )
        }
    }

    fun onFolderSelected(path: String) {
        activity.runOnUiThread {
            webView.evaluateJavascript(
                "if(window.onFolderSelected){window.onFolderSelected('${path.replace("'", "\\'")}')}",
                null
            )
        }
    }

    fun onFolderFilesSelected(files: List<String>) {
        val jsonArray = JSONArray(files)
        activity.runOnUiThread {
            webView.evaluateJavascript(
                "if(window.onFolderFilesSelected){window.onFolderFilesSelected('${jsonArray.toString().replace("'", "\\'")}')}",
                null
            )
        }
    }

    fun destroy() {
        scope.cancel()
    }
}
