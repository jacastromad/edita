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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


// Edita main activity
class EditorActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels()
    lateinit var webView: WebView
    private var darkTheme = mutableStateOf(false)

    // ActivityResultLauncher for opening a document
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            viewModel.setFilename(getFilenameFromUri(it))
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
            viewModel.setModified(value)
        }
    }

    private fun getFilenameFromUri(uri: Uri): String {
        return if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
            NEWFILENAME // Default if content resolver fails
        } else {
            val filename = uri.path ?: ""
            val cut = filename.lastIndexOf('/')
            if (cut != -1) {
                filename.substring(cut + 1)
            } else {
                uri.path ?: NEWFILENAME // Fallback if path parsing fails
            }
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
            updateACEEditor()
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

        val script = StringBuilder().apply {
            append("editor.setTheme(\"ace/theme/$aceTheme\");")
            append("editor.setFontSize(${Preferences.getFontSize()});")
            append("editor.setOption(\"showLineNumbers\", ${Preferences.getNumLines()});")
            append("editor.setOption(\"showGutter\", ${Preferences.getNumLines()});")
            append("editor.setOption(\"enableAutoIndent\", ${Preferences.getAutoIndent()});")
            append("editor.setOption(\"highlightSelectedWord\", ${Preferences.getHlSelection()});")
            append("editor.setOption(\"showInvisibles\", ${Preferences.getShowInvisibles()});")
            append("sessions.forEach(function(session) {")
            append("  session.setOption(\"wrap\", ${Preferences.getWordWrap()});")
            append("});")
        }.toString()

        webView.evaluateJavascript("javascript:$script", null)
    }

    // Creates a new file in the editor
    fun newFile() {
        viewModel.addNewFile()
        viewModel.switchTo(viewModel.filenamesList().lastIndex)
        webView.evaluateJavascript("javascript:addSession('');") { result ->
            webView.evaluateJavascript("javascript:switchSession(${viewModel.activeTab()})", null)
            updateACEEditor()
        }
    }

    // Opens a file picker to load a file into the editor
    fun openFile() {
        webView.evaluateJavascript("javascript:getSessionContent();") { content ->
            if (content.fromJSON() == "" && viewModel.getFilename() == NEWFILENAME && !viewModel.getModified()) {
                openDocumentLauncher.launch(arrayOf("*/*"))
            } else {
                viewModel.addNewFile()
                webView.evaluateJavascript("javascript:addSession('');") { result ->
                    webView.evaluateJavascript("javascript:switchSession(${viewModel.activeTab()})", null)
                    openDocumentLauncher.launch(arrayOf("*/*"))
                    updateACEEditor()
                }
            }
        }
    }

    // Saves the current file content
    fun saveFile() {
        createDocumentLauncher.launch(viewModel.getFilename())
    }

    fun setTab(tab: Int) {
        viewModel.switchTo(tab)
        webView.evaluateJavascript("javascript:switchSession(${viewModel.activeTab()});", null)
    }

    fun closeTab(i: Int) {
        viewModel.closeFile(i)
        webView.evaluateJavascript("javascript:removeSession(${i});") {
            webView.evaluateJavascript("javascript:switchSession(${viewModel.activeTab()});", null)
            updateACEEditor()
        }
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

        val script = StringBuilder().apply {
            append("javascript:setSessionContent(\"${fileContent.toJSON()}\");")
        }.toString()
        webView.evaluateJavascript(script, null)
    }

    // Saves file content to the given URI
    private fun saveFileContent(uri: Uri) {
        var content: String
        if (::webView.isInitialized) {
            webView.evaluateJavascript("javascript:getSessionContent();") { value ->
                content = value?.fromJSON() ?: ""
                contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    Log.d("jscode", "File saved successfully")
                }
                // Use the helper function to set the filename
                viewModel.setFilename(getFilenameFromUri(uri))
                viewModel.setModified(false)
            }
        }
    }

    fun modified(): Boolean = viewModel.getModified()
    fun filename(): String = viewModel.getFilename()
    fun filenamesList(): List<String> = viewModel.filenamesList()
    fun activeTab(): Int = viewModel.activeTab()
}

@Composable
fun FileTabs(
    filenames: List<String>,
    currentIndex: Int,
    onTabSelected: (Int) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (Int) -> Unit
) {
    ScrollableTabRow(selectedTabIndex = currentIndex) {
        // Tabs for each file
        filenames.forEachIndexed { index, title ->
            Tab(
                selected = currentIndex == index,
                onClick = { onTabSelected(index) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title of the tab
                    Text(
                        text = title,
                        modifier = Modifier.padding(end = 8.dp), // Space between text and close icon
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis // Ellipsis if text is too long
                    )

                    // Show close button only on the active tab
                    if (currentIndex == index) {
                        IconButton(
                            onClick = { onCloseTab(index) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Tab"
                            )
                        }
                    }
                }
            }
        }

        // Add Button at the end of the tab row
        IconButton(
            onClick = { onAddTab() }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Tab"
            )
        }
    }
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
                            activity.openFile()
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

                    Log.d("FileTabs", "active: ${activity.activeTab()}")
                    Log.d("FileTabs", "filenames: ${activity.filenamesList()}")
                    Log.d("FileTabs", "modified: ${activity.modified()}")

                    FileTabs(
                        filenames = activity.filenamesList(),
                        currentIndex = activity.activeTab(),
                        onTabSelected = { activity.setTab(it) },
                        onAddTab = { activity.newFile() },
                        onCloseTab = {
                            if (activity.modified()) {
                                showDiscardDialog = true
                                onDialogConfirm = {
                                    activity.closeTab(it)
                                }
                            } else {
                                activity.closeTab(it)
                            }
                        }
                    )

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
            modifier = Modifier.imePadding()
        )
    }
}
