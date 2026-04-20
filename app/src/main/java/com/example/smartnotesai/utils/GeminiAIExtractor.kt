package com.example.smartnotesai.utils

import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.repository.TaskRepository

@Deprecated(
    message = "Use TaskRepository.extractTasksFromAI so the real Gemini flow stays inside the repository."
)
object GeminiAIExtractor {
    suspend fun extractTasks(notes: String): List<Task> = TaskRepository.extractTasksFromAI(notes)
}
