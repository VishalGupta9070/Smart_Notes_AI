package com.example.smartnotesai.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.model.TaskPriority
import com.example.smartnotesai.ui.components.EmptyStateCard
import com.example.smartnotesai.ui.components.InsightCard
import com.example.smartnotesai.ui.components.TaskCard
import com.example.smartnotesai.ui.theme.SmartNotesAITheme

@Composable
fun TaskListScreen(
    date: String,
    onNavigateBack: () -> Unit,
    viewModel: TaskListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(date) {
        viewModel.load(date)
    }

    TaskListScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleTask = viewModel::toggleTask
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListScreenContent(
    uiState: TaskListUiState,
    onNavigateBack: () -> Unit,
    onToggleTask: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = uiState.date.ifBlank { "Task list" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "Unable to load tasks",
                        message = uiState.errorMessage
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        InsightCard(
                            headline = "AI Insight",
                            supportingText = uiState.insight
                        )
                    }

                    if (uiState.tasks.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Nothing scheduled",
                                message = "There are no extracted tasks for this date yet."
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "Open tasks first, completed tasks below",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(uiState.tasks, key = { task -> task.id }) { task ->
                            TaskCard(
                                task = task,
                                onCheckedChange = { onToggleTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListScreenPreview() {
    SmartNotesAITheme {
        TaskListScreenContent(
            uiState = TaskListUiState(
                isLoading = false,
                date = "17 Apr 2026",
                tasks = listOf(
                    Task(
                        id = 1,
                        task = "Call client",
                        priority = TaskPriority.HIGH,
                        deadline = "Tomorrow",
                        date = "17 Apr 2026"
                    ),
                    Task(
                        id = 2,
                        task = "Go to gym",
                        priority = TaskPriority.LOW,
                        deadline = "Not specified",
                        date = "17 Apr 2026",
                        isCompleted = true
                    )
                ),
                insight = "You have 1 high-priority tasks today."
            ),
            onNavigateBack = {},
            onToggleTask = {}
        )
    }
}
