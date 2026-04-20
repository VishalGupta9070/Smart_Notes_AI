package com.example.smartnotesai.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.model.TaskPriority
import com.example.smartnotesai.ui.components.EmptyStateCard
import com.example.smartnotesai.ui.components.SummaryPill
import com.example.smartnotesai.ui.theme.SmartNotesAITheme
import com.example.smartnotesai.utils.TaskDateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTaskList: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate?.let(TaskDateFormatter::toDatePickerMillis)
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            viewModel.updateSelectedDate(
                                TaskDateFormatter.fromDatePickerMillis(selectedDateMillis)
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    HomeScreenContent(
        uiState = uiState,
        modifier = modifier,
        onNavigateToTaskList = onNavigateToTaskList,
        onLogout = onLogout,
        onOpenDatePicker = { showDatePicker = true },
        onClearDateFilter = { viewModel.updateSelectedDate(null) }
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToTaskList: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onClearDateFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OverviewCard(uiState = uiState, onLogout = onLogout)
        }

        item {
            CalendarFilterCard(
                selectedDate = uiState.selectedDate,
                onOpenDatePicker = onOpenDatePicker,
                onClearDateFilter = onClearDateFilter
            )
        }

        uiState.errorMessage?.let { message ->
            item {
                EmptyStateCard(
                    title = "Something went wrong",
                    message = message
                )
            }
        }

        if (uiState.groups.isEmpty()) {
            item {
                EmptyStateCard(
                    title = if (uiState.selectedDate.isNullOrBlank()) {
                        "No tasks yet"
                    } else {
                        "No tasks on ${uiState.selectedDate}"
                    },
                    message = if (uiState.selectedDate.isNullOrBlank()) {
                        "Add a messy note in the Add Notes tab and Smart Notes AI will turn it into a plan."
                    } else {
                        "Try another day or switch back to all dates."
                    }
                )
            }
        } else {
            item {
                Text(
                    text = if (uiState.selectedDate.isNullOrBlank()) {
                        "Tasks grouped by date"
                    } else {
                        "Tasks for ${uiState.selectedDate}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(uiState.groups, key = { group -> group.date }) { group ->
                DateGroupCard(
                    group = group,
                    onClick = { onNavigateToTaskList(group.date) }
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(uiState: HomeUiState, onLogout: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Task intelligence at a glance",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.userEmail.isNotBlank()) {
                        "Signed in as ${uiState.userEmail}"
                    } else {
                        "Capture ideas fast and let the app organize them."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryPill(label = "Total", value = uiState.totalTasks.toString())
                    SummaryPill(label = "High", value = uiState.highPriorityTasks.toString())
                    SummaryPill(label = "Done", value = uiState.completedTasks.toString())
                }
            }
        }
    }
}

@Composable
private fun CalendarFilterCard(
    selectedDate: String?,
    onOpenDatePicker: () -> Unit,
    onClearDateFilter: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Calendar filter",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (selectedDate.isNullOrBlank()) {
                    "Showing tasks from every saved date."
                } else {
                    "Showing only tasks scheduled for $selectedDate."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onOpenDatePicker) {
                    Text(if (selectedDate.isNullOrBlank()) "Pick date" else "Change date")
                }
                if (!selectedDate.isNullOrBlank()) {
                    TextButton(onClick = onClearDateFilter) {
                        Text("All dates")
                    }
                }
            }
        }
    }
}

@Composable
private fun DateGroupCard(
    group: DateGroupUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.date,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${group.totalTasks} tasks \u2022 ${group.highPriorityTasks} high priority",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            group.tasks.take(3).forEach { task ->
                Text(
                    text = "\u2022 ${task.task}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SmartNotesAITheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isLoading = false,
                userEmail = "demo@example.com",
                groups = listOf(
                    DateGroupUiModel(
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
                                task = "Finish UI",
                                priority = TaskPriority.HIGH,
                                deadline = "Friday",
                                date = "17 Apr 2026"
                            ),
                            Task(
                                id = 3,
                                task = "Go to gym",
                                priority = TaskPriority.LOW,
                                deadline = "Not specified",
                                date = "17 Apr 2026"
                            )
                        )
                    )
                )
            ),
            onNavigateToTaskList = {},
            onLogout = {},
            onOpenDatePicker = {},
            onClearDateFilter = {}
        )
    }
}
