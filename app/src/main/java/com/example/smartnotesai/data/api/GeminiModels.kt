package com.example.smartnotesai.data.api

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
) {
    companion object {
        fun create(prompt: String): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = prompt)
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseJsonSchema = linkedMapOf(
                        "type" to "array",
                        "items" to linkedMapOf(
                            "type" to "object",
                            "properties" to linkedMapOf(
                                "task" to linkedMapOf(
                                    "type" to "string",
                                    "description" to "Short actionable task title."
                                ),
                                "priority" to linkedMapOf(
                                    "type" to "string",
                                    "description" to "Task urgency derived from the note.",
                                    "enum" to listOf("High", "Medium", "Low")
                                ),
                                "deadline" to linkedMapOf(
                                    "type" to "string",
                                    "description" to "Exact due phrase from the note, or 'Not specified'."
                                )
                            ),
                            "required" to listOf("task", "priority", "deadline")
                        )
                    )
                )
            )
        }
    }
}

data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val responseJsonSchema: Map<String, Any>? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart> = emptyList()
)

data class GeminiPart(
    val text: String? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val promptFeedback: GeminiPromptFeedback? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

data class GeminiPromptFeedback(
    val blockReason: String? = null
)

data class GeminiErrorEnvelope(
    val error: GeminiError? = null
)

data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

data class ExtractedTask(
    @SerializedName("task") val task: String? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("deadline") val deadline: String? = null
)

fun GeminiResponse.firstText(): String? = candidates
    .asSequence()
    .mapNotNull { it.content }
    .flatMap { it.parts.asSequence() }
    .mapNotNull { it.text?.trim() }
    .firstOrNull { it.isNotEmpty() }
