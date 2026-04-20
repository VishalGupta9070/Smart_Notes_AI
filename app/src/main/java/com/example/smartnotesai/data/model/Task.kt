package com.example.smartnotesai.data.model

data class Task(
    val id: Int,
    val task: String,
    val priority: String,
    val deadline: String,
    val date: String,
    val isCompleted: Boolean = false
)

object TaskPriority {
    const val HIGH = "High"
    const val MEDIUM = "Medium"
    const val LOW = "Low"

    fun rank(priority: String): Int = when (priority) {
        HIGH -> 0
        MEDIUM -> 1
        LOW -> 2
        else -> 3
    }
}
