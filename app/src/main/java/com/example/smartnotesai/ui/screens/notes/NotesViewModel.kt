package com.example.smartnotesai.ui.screens.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.repository.TaskRepository
import com.example.smartnotesai.utils.TaskDateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: String = "",
    val isLoading: Boolean = false,
    val createdTasks: List<Task> = emptyList(),
    val savedDate: String = "",
    val errorMessage: String? = null
) {
    val canExtract: Boolean
        get() = notes.isNotBlank() && !isLoading
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    fun updateNotes(notes: String) {
        _uiState.update { current ->
            current.copy(
                notes = notes,
                errorMessage = null
            )
        }
    }

    fun appendRecognizedSpeech(recognizedText: String) {
        val cleanedSpeech = recognizedText.trim()
        if (cleanedSpeech.isBlank()) {
            showVoiceInputError("No speech detected. Try again.")
            return
        }

        _uiState.update { current ->
            current.copy(
                notes = appendSpeech(current.notes, cleanedSpeech),
                errorMessage = null
            )
        }
    }

    fun showVoiceInputError(message: String) {
        _uiState.update { current ->
            current.copy(errorMessage = message)
        }
    }

    fun extractTasks() {
        val rawNotes = _uiState.value.notes.trim()
        if (rawNotes.isBlank()) {
            _uiState.update { current ->
                current.copy(errorMessage = "Write a note before extracting actions.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                    createdTasks = emptyList()
                )
            }

            runCatching {
                TaskRepository.extractTasksFromAI(rawNotes)
            }.onSuccess { extractedTasks ->
                TaskRepository.addTasks(extractedTasks)
                _uiState.update {
                    NotesUiState(
                        notes = "",
                        isLoading = false,
                        createdTasks = extractedTasks,
                        savedDate = TaskDateFormatter.today()
                    )
                }
            }.onFailure { exception ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = "AI extraction failed: ${exception.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun clearResult() {
        _uiState.update { current ->
            current.copy(createdTasks = emptyList(), savedDate = "")
        }
    }

    private fun appendSpeech(existingNotes: String, recognizedText: String): String {
        val currentNotes = existingNotes.trimEnd()
        return if (currentNotes.isBlank()) {
            recognizedText
        } else {
            "$currentNotes\n$recognizedText"
        }
    }
}
