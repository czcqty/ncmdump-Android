package com.ncmdump.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.webkit.WebViewAssetLoader
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bridge: WebViewBridge

    // File picker launchers
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var folderFilePickerLauncher: ActivityResultLauncher<Intent>
    private var folderFilePickerExt: String = "ncm"

    // Permission launcher
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var manageStorageLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup activity result launchers
        setupLaunchers()

        // Request storage permissions
        requestStoragePermission()

        // Enable WebView debugging
        WebView.setWebContentsDebuggingEnabled(true)

        // Use WebViewAssetLoader to serve assets via https:// protocol
        // This avoids all CORS and ES module issues with file:// protocol
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        // Create WebView programmatically (no XML layout needed)
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // Enable viewport meta tag
                useWideViewPort = true
                loadWithOverviewMode = true
                // Disable zoom
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }
            // Intercept requests and serve assets via WebViewAssetLoader
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                        ?: super.shouldInterceptRequest(view, request)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("NcmDumpWebView", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                    return true
                }
            }
        }

        // Create bridge and bind to WebView
        bridge = WebViewBridge(this, webView)
        webView.addJavascriptInterface(bridge, "Android")

        setContentView(webView)

        // Load frontend via WebViewAssetLoader (https:// protocol)
        // Assets are at: app/src/main/assets/frontend/index.html
        // WebViewAssetLoader maps /assets/ -> assets/ folder
        webView.loadUrl("https://appassets.androidplatform.net/assets/frontend/index.html")
    }

    private fun setupLaunchers() {
        // File picker for .ncm files
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val files = mutableListOf<String>()
                val data = result.data

                // Handle multiple file selection
                val clipData = data?.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        val path = getPathFromUri(uri)
                        if (path != null) files.add(path)
                    }
                } else {
                    // Single file
                    val uri = data?.data
                    if (uri != null) {
                        val path = getPathFromUri(uri)
                        if (path != null) files.add(path)
                    }
                }

                bridge.onFilesSelected(files)
            } else {
                bridge.onFilesSelected(emptyList())
            }
        }

        // Folder picker for save path
        folderPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val path = getPathFromUri(uri)
                    bridge.onFolderSelected(path ?: "")
                } else {
                    bridge.onFolderSelected("")
                }
            } else {
                bridge.onFolderSelected("")
            }
        }

        // Folder picker for listing files
        folderFilePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val path = getPathFromUri(uri)
                    if (path != null) {
                        val files = listFilesRecursively(File(path), folderFilePickerExt)
                        bridge.onFolderFilesSelected(files)
                    } else {
                        // Try DocumentFile approach for SAF URIs
                        val docFile = DocumentFile.fromTreeUri(this, uri)
                        if (docFile != null) {
                            val files = listDocumentFilesRecursively(docFile, folderFilePickerExt)
                            bridge.onFolderFilesSelected(files)
                        } else {
                            bridge.onFolderFilesSelected(emptyList())
                        }
                    }
                } else {
                    bridge.onFolderFilesSelected(emptyList())
                }
            } else {
                bridge.onFolderFilesSelected(emptyList())
            }
        }

        // Permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Check if granted
        }

        manageStorageLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { /* result handled */ }
    }

    // ---- Public methods called from WebViewBridge ----

    fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        filePickerLauncher.launch(intent)
    }

    fun launchFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }

    fun launchFolderFilePicker(ext: String) {
        folderFilePickerExt = ext
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderFilePickerLauncher.launch(intent)
    }

    // ---- File path utilities ----

    /**
     * Get real file path from content URI
     */
    private fun getPathFromUri(uri: Uri): String? {
        // Try to get the document ID and resolve to a real path
        try {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)

                // ExternalStorageProvider
                if ("com.android.externalstorage.documents" == uri.authority) {
                    val split = docId.split(":")
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                    }
                    // Handle SD card
                    val externalDirs = getExternalFilesDirs(null)
                    for (dir in externalDirs) {
                        if (dir != null) {
                            val storageRoot = dir.absolutePath.substringBefore("/Android")
                            if (storageRoot.contains(type, ignoreCase = true) || type == "home") {
                                return "$storageRoot/${split.getOrElse(1) { "" }}"
                            }
                        }
                    }
                }

                // DownloadsProvider
                if ("com.android.providers.downloads.documents" == uri.authority) {
                    if (docId.startsWith("raw:")) {
                        return docId.substringAfter("raw:")
                    }
                }
            }

            // Tree URI (folder selection)
            if (DocumentsContract.isTreeUri(uri)) {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                if ("com.android.externalstorage.documents" == uri.authority) {
                    val split = docId.split(":")
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return "${Environment.getExternalStorageDirectory()}/${split.getOrElse(1) { "" }}"
                    }
                }
            }

            // Fallback: try content resolver
            val cursor = contentResolver.query(uri, arrayOf("_data"), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex("_data")
                    if (idx >= 0) {
                        return it.getString(idx)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return uri.path
    }

    /**
     * List files recursively with given extension
     * Port of: utils.ListFilesFromFolder in Go
     */
    private fun listFilesRecursively(dir: File, ext: String): List<String> {
        val result = mutableListOf<String>()
        if (!dir.exists() || !dir.isDirectory) return result

        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                result.addAll(listFilesRecursively(file, ext))
            } else if (file.name.endsWith(".$ext", ignoreCase = true)) {
                result.add(file.absolutePath)
            }
        }
        return result
    }

    /**
     * List DocumentFile objects recursively (for SAF URIs)
     */
    private fun listDocumentFilesRecursively(dir: DocumentFile, ext: String): List<String> {
        val result = mutableListOf<String>()
        dir.listFiles().forEach { file ->
            if (file.isDirectory) {
                result.addAll(listDocumentFilesRecursively(file, ext))
            } else if (file.name?.endsWith(".$ext", ignoreCase = true) == true) {
                val uri = file.uri
                val path = getPathFromUri(uri)
                if (path != null) {
                    result.add(path)
                }
            }
        }
        return result
    }

    // ---- Permissions ----

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - request MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                permissionLauncher.launch(needed.toTypedArray())
            }
        }
    }

    override fun onDestroy() {
        bridge.destroy()
        webView.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
