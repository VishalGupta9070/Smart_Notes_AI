package com.example.smartnotesai.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.model.TaskPriority
import com.example.smartnotesai.data.repository.TaskRepository
import com.example.smartnotesai.utils.TaskDateFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TaskListUiState(
    val isLoading: Boolean = true,
    val date: String = "",
    val tasks: List<Task> = emptyList(),
    val insight: String = "",
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel(application: Application) : AndroidViewModel(application) {
    private val selectedDate = MutableStateFlow("")

    val uiState: StateFlow<TaskListUiState> = selectedDate
        .flatMapLatest { date ->
            if (date.isBlank()) {
                flowOf(TaskListUiState())
            } else {
                TaskRepository.tasks.map {
                    val dateTasks = TaskRepository.getTasksByDate(date)

                    TaskListUiState(
                        isLoading = false,
                        date = date,
                        tasks = dateTasks,
                        insight = buildInsight(date, dateTasks)
                    )
                }
            }
        }
        .catch {
            emit(
                TaskListUiState(
                    isLoading = false,
                    date = selectedDate.value,
                    errorMessage = "Unable to load tasks for this date."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TaskListUiState()
        )

    fun load(date: String) {
        if (selectedDate.value != date) {
            selectedDate.value = date
        }
    }

    fun toggleTask(taskId: Int) {
        TaskRepository.toggleTaskCompletion(taskId)
    }

    private fun buildInsight(date: String, tasks: List<Task>): String {
        val openHighPriorityTasks = tasks.count {
            it.priority == TaskPriority.HIGH && !it.isCompleted
        }
        return if (TaskDateFormatter.isToday(date)) {
            "You have $openHighPriorityTasks high-priority tasks today."
        } else {
            "You have $openHighPriorityTasks high-priority tasks in this date bucket."
        }
    }
}
