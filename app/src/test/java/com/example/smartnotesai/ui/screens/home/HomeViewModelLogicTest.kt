package com.example.smartnotesai.ui.screens.home

import com.example.smartnotesai.data.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelLogicTest {

    @Test
    fun buildHomeGroupsKeepsNewestDatesFirstAndPreservesTaskOrderWithinDate() {
        val groups = buildHomeGroups(
            listOf(
                task(id = 1, title = "Call client", date = "20 Apr 2026"),
                task(id = 2, title = "Finish UI", date = "20 Apr 2026", isCompleted = true),
                task(id = 3, title = "Pay invoice", date = "21 Apr 2026")
            )
        )

        assertEquals(listOf("21 Apr 2026", "20 Apr 2026"), groups.map(DateGroupUiModel::date))
        assertEquals(listOf(1, 2), groups.last().tasks.map(Task::id))
    }

    private fun task(
        id: Int,
        title: String,
        date: String,
        isCompleted: Boolean = false
    ): Task {
        return Task(
            id = id,
            task = title,
            priority = "Medium",
            deadline = "Not specified",
            date = date,
            isCompleted = isCompleted
        )
    }
}
