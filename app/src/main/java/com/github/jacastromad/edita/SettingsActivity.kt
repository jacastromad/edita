package com.github.jacastromad.edita

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.jacastromad.edita.ui.theme.EditaTheme

// Main activity for the settings screen
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize preferences
        Preferences.init(this)

        // Set up the UI using Jetpack Compose
        setContent {
            EditaTheme {
                SettingsScreen(goBack = { finish() })
            }
        }
    }
}

// Main composable function for the settings screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(goBack: () -> Unit) {
    // State variables for various settings
    var fontSizeStr by remember { mutableStateOf(Preferences.getFontSize().toString()) }
    var theme by remember { mutableStateOf(Preferences.getTheme()) }
    var expanded by remember { mutableStateOf(false) }
    var numLines by remember { mutableStateOf(Preferences.getNumLines()) }
    var autoIndent by remember { mutableStateOf(Preferences.getAutoIndent()) }
    var wordWrap by remember { mutableStateOf(Preferences.getWordWrap()) }
    var hlSelection by remember { mutableStateOf(Preferences.getHlSelection()) }
    var showInv by remember { mutableStateOf(Preferences.getShowInvisibles()) }
    var softTabs by remember { mutableStateOf(Preferences.getSoftTabs()) }
    var tabStopStr by remember { mutableStateOf(Preferences.getTabStop().toString()) }

    // Get the system theme (true if dark mode)
    val systemTheme = isSystemInDarkTheme()

    // Determine if dark theme should be used
    val isDarkTheme by remember {
        derivedStateOf {
            when (theme) {
                "dark" -> true
                "light" -> false
                else -> systemTheme
            }
        }
    }

    EditaTheme (darkTheme = isDarkTheme) {
        Scaffold(
            topBar = {  // Top app bar with title and back button
                TopAppBar(
                    title = { Text(text = "Settings") },
                    navigationIcon = {
                        IconButton(onClick = {
                            goBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back"
                            )
                        }
                    }
                )
            },
            content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    // Font size setting
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Font Size:", modifier = Modifier.padding(end = 8.dp))
                        OutlinedTextField(
                            value = fontSizeStr,
                            onValueChange = {
                                fontSizeStr = it
                                val size = it.toFloatOrNull()
                                if (size != null && size in 2f..50f) {
                                    Preferences.setFontSize(size)
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.width(80.dp)
                        )
                    }
                    // Line numbers setting
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = numLines,
                            onCheckedChange = {
                                numLines = it
                                Preferences.setNumLines(it)
                            }
                        )
                        Text(text = "Line Numbers", modifier = Modifier.padding(start = 8.dp))
                    }
                    // Auto indentation setting
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = autoIndent,
                            onCheckedChange = {
                                autoIndent = it
                                Preferences.setAutoIndent(it)
                            }
                        )
                        Text(text = "Auto Indentation", modifier = Modifier.padding(start = 8.dp))
                    }
                    // Word wrap setting
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = wordWrap,
                            onCheckedChange = {
                                wordWrap = it
                                Preferences.setWordWrap(it)
                            }
                        )
                        Text(text = "Word Wrap", modifier = Modifier.padding(start = 8.dp))
                    }
                    // Highlight selection setting
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = hlSelection,
                            onCheckedChange = {
                                hlSelection = it
                                Preferences.setHlSelection(it)
                            }
                        )
                        Text(text = "Highlight Selection", modifier = Modifier.padding(start = 8.dp))
                    }
                    // Show invisibles setting
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showInv,
                            onCheckedChange = {
                                showInv = it
                                Preferences.setShowInvisibles(it)
                            }
                        )
                        Text(text = "Show Invisibles", modifier = Modifier.padding(start = 8.dp))
                    }
                    // Soft tabs setting
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = softTabs,
                            onCheckedChange = {
                                softTabs = it
                                Preferences.setSoftTabs(it)
                            }
                        )
                        Text(text = "Soft Tabs", modifier = Modifier.padding(start = 8.dp))
                    }
                    // Tab stop setting
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Tab Stop:", modifier = Modifier.padding(end = 8.dp))
                        OutlinedTextField(
                            value = tabStopStr,
                            onValueChange = {
                                tabStopStr = it
                                val stop = it.toIntOrNull()
                                if (stop != null && stop >= 1) {
                                    Preferences.setTabStop(stop)
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.width(80.dp)
                        )
                    }
                    // Theme setting
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Theme:", modifier = Modifier.padding(end = 8.dp))
                        Box {
                            Button(onClick = { expanded = true }) {
                                Text(text = theme)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        theme = "light"
                                        Preferences.setTheme(theme)
                                        expanded = false
                                    },
                                    text = { Text("light") }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        theme = "dark"
                                        Preferences.setTheme(theme)
                                        expanded = false
                                    },
                                    text = { Text("dark") }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        theme = "system"
                                        Preferences.setTheme(theme)
                                        expanded = false
                                    },
                                    text = { Text("system") }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
