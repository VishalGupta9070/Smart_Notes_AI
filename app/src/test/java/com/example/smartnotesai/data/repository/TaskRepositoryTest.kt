package com.example.smartnotesai.data.repository

import com.example.smartnotesai.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class TaskRepositoryTest {

    @Test
    fun testGeminiApiIntegration() = runBlocking {
        // Skip test if no API key is provided
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            println("Skipping test: GEMINI_API_KEY is blank")
            return@runBlocking
        }

        val testNotes = "Call client tomorrow, finish UI by Friday"
        println("Testing Gemini API with input: '$testNotes'")

        try {
            val tasks = TaskRepository.extractTasksFromAI(testNotes)
            
            assertNotNull("Task list should not be null", tasks)
            assertTrue("Task list should not be empty", tasks.isNotEmpty())
            
            println("API Working! Extracted ${tasks.size} tasks:")
            tasks.forEachIndexed { index, task ->
                println("${index + 1}. [${task.priority}] ${task.task} (Deadline: ${task.deadline})")
            }
            
            // Basic validation of content
            val taskNames = tasks.map { it.task.lowercase() }
            assertTrue("Should extract 'call client'", taskNames.any { it.contains("call client") })
            assertTrue("Should extract 'finish ui'", taskNames.any { it.contains("finish ui") })
            
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("HTTP 429") || message.contains("HTTP 503")) {
                println("Skipping Gemini API test due to temporary service/quota condition: $message")
                return@runBlocking
            }
            fail("API Call Failed: ${e.message}")
        }
    }
}
