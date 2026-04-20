package com.example.smartnotesai.utils

import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.model.TaskPriority
import kotlinx.coroutines.delay
import java.util.Locale

object MockAIExtractor {
    private val splitter = Regex("[,;\\n]+")
    private val weekdayPattern = Regex(
        "\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
        RegexOption.IGNORE_CASE
    )
    private val timeHintPattern = Regex(
        "\\b(by|on|before|this|next)?\\s*(today|tomorrow|tonight|next week|weekend|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
        RegexOption.IGNORE_CASE
    )
    private val softenerPattern = Regex(
        "\\b(maybe|probably|sometime|whenever|if possible|try to|remember to|please|need to|i need to|we need to)\\b",
        RegexOption.IGNORE_CASE
    )

    suspend fun extractTasks(notes: String): List<Task> {
        delay(1_200)

        val createdDate = TaskDateFormatter.today()
        val candidates = splitIntoCandidates(notes)
        val extractedTasks = candidates.mapNotNull { candidate ->
            val trimmed = candidate.trim().trim('.', '!', '?')
            if (trimmed.isBlank()) {
                null
            } else {
                val cleanedTask = cleanTask(trimmed)
                if (cleanedTask.isBlank()) {
                    null
                } else {
                    Task(
                        id = 0,
                        task = cleanedTask,
                        priority = inferPriority(trimmed, extractDeadline(trimmed)),
                        deadline = extractDeadline(trimmed),
                        date = createdDate
                    )
                }
            }
        }

        if (extractedTasks.isNotEmpty()) return extractedTasks

        return listOf(
            Task(
                id = 0,
                task = cleanTask(notes.ifBlank { "Review note" }),
                priority = TaskPriority.MEDIUM,
                deadline = "Not specified",
                date = createdDate
            )
        )
    }

    private fun splitIntoCandidates(notes: String): List<String> {
        val normalized = notes
            .replace(Regex("\\band then\\b", RegexOption.IGNORE_CASE), ",")
            .replace(Regex("\\bthen\\b", RegexOption.IGNORE_CASE), ",")
            .replace(Regex("\\balso\\b", RegexOption.IGNORE_CASE), ",")

        return normalized.split(splitter).map(String::trim).filter(String::isNotBlank)
    }

    private fun extractDeadline(candidate: String): String {
        val lowered = candidate.lowercase(Locale.ENGLISH)
        return when {
            lowered.contains("today") -> "Today"
            lowered.contains("tomorrow") -> "Tomorrow"
            lowered.contains("tonight") -> "Tonight"
            lowered.contains("next week") -> "Next week"
            lowered.contains("weekend") -> "Weekend"
            else -> weekdayPattern.find(candidate)?.value
                ?.replaceFirstChar { character -> character.titlecase(Locale.ENGLISH) }
                ?: "Not specified"
        }
    }

    private fun inferPriority(candidate: String, deadline: String): String {
        val lowered = candidate.lowercase(Locale.ENGLISH)
        return when {
            lowered.contains("maybe") || lowered.contains("if possible") || lowered.contains("sometime") -> TaskPriority.LOW
            deadline != "Not specified" -> TaskPriority.HIGH
            lowered.contains("urgent") || lowered.contains("asap") -> TaskPriority.HIGH
            lowered.contains("call") || lowered.contains("finish") || lowered.contains("submit") || lowered.contains("pay") -> TaskPriority.HIGH
            lowered.contains("plan") || lowered.contains("review") || lowered.contains("prepare") -> TaskPriority.MEDIUM
            else -> TaskPriority.MEDIUM
        }
    }

    private fun cleanTask(candidate: String): String {
        var cleaned = candidate
        listOf(
            "maybe",
            "please",
            "remember to",
            "need to",
            "i need to",
            "we need to",
            "have to",
            "must",
            "try to"
        ).forEach { phrase ->
            cleaned = cleaned.replace(
                Regex("^${Regex.escape(phrase)}\\s+", RegexOption.IGNORE_CASE),
                ""
            )
        }
        cleaned = cleaned.replace(timeHintPattern, " ")
        cleaned = cleaned.replace(softenerPattern, " ")
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim(' ', '.', '!', '?', '-')

        if (cleaned.isBlank()) return "Untitled task"

        return cleaned.replaceFirstChar { character ->
            if (character.isLowerCase()) {
                character.titlecase(Locale.ENGLISH)
            } else {
                character.toString()
            }
        }
    }
}
