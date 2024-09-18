package com.github.jacastromad.edita

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

const val NEWFILENAME = "Untitled"

class EditorViewModel : ViewModel() {

    // Data class to represent each file's state
    data class FileState(
        var modified: Boolean = false,
        var filename: String = NEWFILENAME
    )

    // Map to hold multiple file states, each with a unique tab ID
    private val files = mutableStateListOf<FileState>()

    // Track the active file (tab)
    private var index by mutableIntStateOf(0)

    // Initialize the first file state
    init {
        files.add(FileState())
    }

    // Function to switch active tab
    fun switchTo(newIndex: Int) {
        if (newIndex in files.indices) {
            index = newIndex
        }
    }

    // Getters for current file (files[index] should never be null)
    fun getFilename(): String = files[index]!!.filename
    fun getModified(): Boolean = files[index]!!.modified

    fun setFilename(filename: String) {
        files[index] = files[index].copy(filename = filename)
    }
    fun setModified(modified: Boolean) {
        files[index] = files[index].copy(modified = modified)
        Log.d("MODIFIED", "$modified")
    }

    // Function to add a new tab (file)
    fun addNewFile() {
        files.add(FileState())
        index = files.lastIndex
    }

    // Close current file
    fun closeFile(i: Int) {
        if (i in files.indices) {
            files.removeAt(i)
            if (files.size == 0) {
                addNewFile()
            }
            index = if (i > 0) i-1 else 0
        }
    }

    // Return a list of filenames
    fun filenamesList(): List<String> = files.map {
        "${it.filename}" + if (it.modified) " *" else ""
    }

    // Return current tab (index)
    fun activeTab(): Int = index
}


