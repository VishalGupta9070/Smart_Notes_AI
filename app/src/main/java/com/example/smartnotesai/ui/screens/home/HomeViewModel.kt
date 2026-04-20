package com.example.smartnotesai.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.model.TaskPriority
import com.example.smartnotesai.data.repository.AuthRepository
import com.example.smartnotesai.data.repository.TaskRepository
import com.example.smartnotesai.utils.TaskDateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DateGroupUiModel(
    val date: String,
    val tasks: List<Task>
) {
    val totalTasks: Int
        get() = tasks.size

    val highPriorityTasks: Int
        get() = tasks.count { it.priority == TaskPriority.HIGH }

    val completedTasks: Int
        get() = tasks.count { it.isCompleted }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val userEmail: String = "",
    val selectedDate: String? = null,
    val groups: List<DateGroupUiModel> = emptyList(),
    val errorMessage: String? = null
) {
    val totalTasks: Int
        get() = groups.sumOf { it.totalTasks }

    val highPriorityTasks: Int
        get() = groups.sumOf { it.highPriorityTasks }

    val completedTasks: Int
        get() = groups.sumOf { it.completedTasks }
}

internal fun buildHomeGroups(tasks: List<Task>): List<DateGroupUiModel> {
    return tasks
        .groupBy { task -> task.date }
        .map { (date, tasksForDay) ->
            DateGroupUiModel(
                date = date,
                tasks = tasksForDay
            )
        }
        .sortedByDescending { group ->
            TaskDateFormatter.toSortableEpochDay(group.date)
        }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val selectedDate = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        TaskRepository.tasks,
        selectedDate
    ) { tasks, selectedDate ->
        val visibleTasks = if (selectedDate.isNullOrBlank()) {
            tasks
        } else {
            TaskRepository.filterTasksByDate(tasks, selectedDate)
        }

        HomeUiState(
            isLoading = false,
            userEmail = authRepository.getSavedEmail(),
            selectedDate = selectedDate,
            groups = buildHomeGroups(visibleTasks)
        )
    }
        .catch {
            emit(
                HomeUiState(
                    isLoading = false,
                    userEmail = authRepository.getSavedEmail(),
                    selectedDate = selectedDate.value,
                    errorMessage = "Unable to load your saved tasks."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(userEmail = authRepository.getSavedEmail())
        )

    fun updateSelectedDate(date: String?) {
        selectedDate.value = date?.takeIf { it.isNotBlank() }
    }
}
