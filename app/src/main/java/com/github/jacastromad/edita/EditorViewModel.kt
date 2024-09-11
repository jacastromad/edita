package com.github.jacastromad.edita

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

class EditorViewModel : ViewModel() {

    // Data class to represent each file's state
    data class FileState(
        var content: String = "",
        var modified: Boolean = false,
        var filename: String = "Untitled"
    )

    // Map to hold multiple file states, each with a unique tab ID
    private val files = mutableMapOf<Int, MutableState<FileState>>()

    // Track the active file (tab)
    private var index by mutableIntStateOf(0)

    // Initialize the first file state
    init {
        files[index] = mutableStateOf(FileState())
    }

    // Function to switch active tab
    fun switchTo(newIndex: Int) {
        if (files.containsKey(newIndex)) {
            index = newIndex
        }
    }

    // Getters for current file (files[index] should never be null)
    fun getContent(): String = files[index]!!.value.content
    fun getFilename(): String = files[index]!!.value.filename
    fun getModified(): Boolean = files[index]!!.value.modified

    private fun updateCurrentFile(update: (FileState) -> FileState) {
        files[index]?.value = files[index]?.value?.let(update) ?: return
    }

    // Setters for current file
    fun setContent(content: String) = updateCurrentFile { it.copy(content = content) }
    fun setFilename(filename: String) = updateCurrentFile { it.copy(filename = filename) }
    fun setModified(modified: Boolean) = updateCurrentFile { it.copy(modified = modified) }

    // Function to add a new tab (file)
    fun addNewFile() {
        val newIndex = files.size
        files[newIndex] = mutableStateOf(FileState())
        index = newIndex
    }

    // Close current file
    fun closeFile() {
        files.remove(index)
        if (files.isEmpty()) {
            addNewFile() // Always ensure at least one empty file exists
        } else {
            // Switch to the first remaining file
            index = files.keys.first()
        }
    }

    // Return a list of filenames
    fun filenamesList(): List<String> = files.values.map {
        "${it.value.filename} " + if (it.value.modified) "* " else ""
    }

    // Return current tab (index)
    fun activeTab(): Int = index
}


