package com.github.jacastromad.edita

import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.github.jacastromad.edita.ui.theme.EditaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.github.jacastromad.edita.EditorViewModel
import androidx.activity.viewModels

private const val NEWFILE = "Untitled"

// Edita main activity
class EditorActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()
    lateinit var webView: WebView
    var modified by mutableStateOf(false)
    var fileName by mutableStateOf(NEWFILE)
    private var fileUri by mutableStateOf<Uri?>(null)
    private var darkTheme = mutableStateOf(false)

    // ActivityResultLauncher for opening a document
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            fileUri = it
            fileName = it.path?.substringAfterLast('/') ?: NEWFILE

            // Retrieve the file name from the content provider or file path
            if (it.scheme == "content") {
                val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
                cursor.use {
                    if (it != null && it.moveToFirst()) {
                        fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            } else {
                fileName = it.path ?: ""
                val cut = fileName.lastIndexOf('/')
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1)
                }
            }

            loadFileContent(it)
        }
    }

    // ActivityResultLauncher for creating a document
    private val createDocumentLauncher = registerForActivityResult(CreateDocument("*/*")) { uri: Uri? ->
        uri?.let {
            saveFileContent(it)
        }
    }

    // ActivityResultLauncher for settings activity
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateEditor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Preferences.init(this)

        setContent {
            EditaTheme {
                Editor(darkTheme = darkTheme)
            }
        }
    }

    // Handles configuration change of the system theme
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (Preferences.getTheme() == "system") {
            val aceTheme = when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> "twilight"
                Configuration.UI_MODE_NIGHT_NO -> "dawn"
                else -> "dawn"
            }
            webView.evaluateJavascript(
                "javascript:editor.setTheme(\"ace/theme/${aceTheme}\");",
                null
            )
            darkTheme.value = when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }
        }
    }

    // JavaScript Interface to communicate between WebView and Android
    inner class JavaScriptInterface {
        @android.webkit.JavascriptInterface
        fun setModified(value: Boolean) {
            modified = value
        }
    }

    // Launches the SettingsActivity
    fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        settingsLauncher.launch(intent)
    }

    // Updates ACE editor settings and the main activity theme
    fun updateEditor() {
        lifecycleScope.launch {
            while (!::webView.isInitialized) {
                delay(50)
            }
            updateACEEditor()
            val context = this@EditorActivity
            if (Preferences.getTheme() == "system") {
                darkTheme.value = when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> true
                    Configuration.UI_MODE_NIGHT_NO -> false
                    else -> false
                }
            } else {
                darkTheme.value = Preferences.getTheme() == "dark"
            }
        }
    }

    // Updates ACE editor settings based on Preferences
    private fun updateACEEditor() {
        val aceTheme = when (Preferences.getTheme()) {
            "system" -> when (this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> "twilight"
                Configuration.UI_MODE_NIGHT_NO -> "dawn"
                else -> "dawn"
            }
            "light" -> "dawn"
            "dark" -> "twilight"
            else -> "dawn"
        }
        webView.evaluateJavascript(
            "javascript:editor.setTheme(\"ace/theme/${aceTheme}\");",
            null
        )
        webView.evaluateJavascript(
            "javascript:editor.setFontSize(${Preferences.getFontSize()});",
            null
        )
        webView.evaluateJavascript(
            "javascript:editor.setOption(\"showLineNumbers\", ${Preferences.getNumLines()});",
            null
        )
        webView.evaluateJavascript(
                "javascript:editor.setOption(\"showGutter\", ${Preferences.getNumLines()});",
        null
        )
        webView.evaluateJavascript(
            "javascript:editor.setOption(\"enableAutoIndent\", ${Preferences.getAutoIndent()});",
            null
        )
        webView.evaluateJavascript(
            "javascript:editor.setOption(\"highlightSelectedWord\", ${Preferences.getHlSelection()});",
            null
        )
        webView.evaluateJavascript(
            "javascript:editor.setOption(\"showInvisibles\", ${Preferences.getShowInvisibles()});",
            null
        )
        webView.evaluateJavascript(
            "javascript:editor.session.setUseWrapMode(${Preferences.getWordWrap()});",
            null
        )
    }

    // Creates a new file in the editor
    fun newFile() {
        fileName = NEWFILE
        fileUri = null
        webView.evaluateJavascript("javascript:setEditorContent(\"\");", null)
        webView.evaluateJavascript("javascript:editor.getSession().getUndoManager().reset();", null)
        modified = false
    }

    // Opens a file picker to load a file into the editor
    fun openFile() {
        openDocumentLauncher.launch(arrayOf("*/*"))
    }

    // Saves the current file content
    fun saveFile() {
        createDocumentLauncher.launch(fileName)
    }

    // Calls ACE editor undo
    fun undo() {
        webView.evaluateJavascript("javascript:editor.undo();", null)
    }

    // Calls ACE editor redo
    fun redo() {
        webView.evaluateJavascript("javascript:editor.redo();", null)
    }

    // Calls ACE editor find
    fun find(query: String) {
        webView.evaluateJavascript("javascript:editor.find('$query');", null)
    }

    // Loads file content into the WebView
    private fun loadFileContent(uri: Uri) {
        // Use a content resolver to open the input stream for the file and read its contents
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val fileContent = inputStream?.bufferedReader().use { it?.readText() } ?: ""
        inputStream?.close()

        val content = fileContent.toJSON()

        webView.evaluateJavascript("javascript:setEditorContent(\"$content\");", null)
        webView.evaluateJavascript("javascript:editor.getSession().getUndoManager().reset();", null)
    }

    // Saves file content to the given URI
    private fun saveFileContent(uri: Uri) {
        var content: String
        if (::webView.isInitialized) {
            webView.evaluateJavascript("javascript:editor.getValue();") { value ->
                content = value?.fromJSON() ?: ""
                contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    Log.d("Edita", "File saved successfully")
                }
            }

            // TODO: duplicated code (openDocumentLauncher)
            if (uri.scheme == "content") {
                val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
                cursor.use {
                    if (it != null && it.moveToFirst()) {
                        fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            } else {
                fileName = uri.path ?: ""
                val cut = fileName.lastIndexOf('/')
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1)
                }
            }
            modified = false
        }
    }
}

// Composable function to create the WebView with the ACE editor.
@Composable
fun EditorWebView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as EditorActivity

    // URL of the ACE editor html
    val editorHTML = "file:///android_asset/editor.html"

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Add JavaScript interface
                addJavascriptInterface(activity.JavaScriptInterface(), "Android")
                activity.webView = this
                loadUrl(editorHTML)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        activity.updateEditor()
                    }
                }
            }
        },
        update = { },
        modifier = modifier
    )
}

// Composable function to create the find bar.
@Composable
fun FindBar(
    findQuery: String,
    onFindQueryChange: (String) -> Unit,
    onFind: () -> Unit,
    onCloseBar: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
    ) {
        TextField(
            value = findQuery,
            onValueChange = onFindQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search...") }
        )
        IconButton(onClick = onFind) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Find"
            )
        }
        IconButton(onClick = onCloseBar) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close Search"
            )
        }
    }
}

// Edita Main composable function
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Editor(darkTheme: MutableState<Boolean>) {
    val context = LocalContext.current
    val activity = context as EditorActivity
    var showMenu by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var onDialogConfirm by remember { mutableStateOf<() -> Unit> ({}) }
    var showFindArea by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    EditaTheme (darkTheme = darkTheme.value) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Edita") },
                    actions = {
                        IconButton(onClick = {
                            if (activity.modified) {
                                showDiscardDialog = true
                                onDialogConfirm = {
                                    activity.newFile()
                                }
                            } else {
                                activity.newFile()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Create,
                                contentDescription = "New file",
                            )
                        }
                        IconButton(onClick = {
                            if (activity.modified) {
                                showDiscardDialog = true
                                onDialogConfirm = {
                                    activity.openFile()
                                }
                            } else {
                                activity.openFile()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FileOpen,
                                contentDescription = "Open file",
                            )
                        }
                        IconButton(onClick = {
                            activity.saveFile()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Save file",
                            )
                        }
                        Box {
                            IconButton(onClick = {
                                showMenu = !showMenu
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "more options",
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row {
                                            Icon(
                                                imageVector = Icons.Filled.Search,
                                                contentDescription = "Find",
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text("Find")
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        showFindArea = true
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = {
                                        Row {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = "Undo",
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text("Undo")
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        activity.undo()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                                contentDescription = "Redo",
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text("Redo")
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        activity.redo()
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = {
                                        Row {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = "Settings",
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text("Settings")
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        activity.openSettings()
                                    }
                                )
                            }
                        }
                    }
                )
            },
            content = { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    if (showFindArea) {
                        FindBar(
                            findQuery = findQuery,
                            onFindQueryChange = { findQuery = it },
                            onFind = { activity.find(findQuery) },
                            onCloseBar = { showFindArea = false }
                        )
                    }

                    EditorWebView(Modifier.weight(1f))

                    if (showDiscardDialog) {
                        AlertDialog(
                            onDismissRequest = { showDiscardDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDiscardDialog = false
                                    onDialogConfirm()
                                }) {
                                    Text("Discard Changes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDiscardDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                            title = { Text("Unsaved Changes") },
                            text = { Text("You have unsaved changes. Do you really want to discard them?") }
                        )
                    }
                }
            },
            bottomBar = {
                BottomAppBar(
                    content = {
                        Column {
                            Text(text = "File: ${activity.fileName}")
                            Text(text = if (activity.modified) "*** Unsaved changes ***" else "")
                        }
                    }
                )
            },
            modifier = Modifier.imePadding()
        )
    }
}
