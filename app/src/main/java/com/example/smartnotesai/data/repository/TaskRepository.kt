package com.example.smartnotesai.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.smartnotesai.BuildConfig
import com.example.smartnotesai.data.api.ExtractedTask
import com.example.smartnotesai.data.api.GeminiApiService
import com.example.smartnotesai.data.api.GeminiErrorEnvelope
import com.example.smartnotesai.data.api.GeminiRequest
import com.example.smartnotesai.data.api.GeminiResponse
import com.example.smartnotesai.data.api.firstText
import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.utils.TaskDateFormatter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TaskRepository {
    private const val TAG = "GEMINI_DEBUG"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val GEMINI_MODEL = "models/gemini-2.5-flash"
    private const val MAX_GEMINI_RETRIES = 3
    private const val PREFS_NAME = "smart_notes_tasks"
    private const val KEY_TASKS = "saved_tasks"

    private val gson = Gson()
    private val extractedTaskListType = object : TypeToken<List<ExtractedTask>>() {}.type

    private val apiService: GeminiApiService by lazy {
        val debugInterceptor = Interceptor { chain ->
            val request = chain.request()
            val requestBody = request.body?.let(::readRequestBody).orEmpty()

            Log.d(TAG, "HTTP Request: ${request.method} ${request.url}")
            if (requestBody.isNotBlank()) {
                Log.d(TAG, "HTTP Request Body: $requestBody")
            }

            val response = chain.proceed(request)
            val rawResponseBody = runCatching {
                response.peekBody(Long.MAX_VALUE).string()
            }.getOrElse { error ->
                "<unable to read response body: ${error.message}>"
            }

            Log.d(TAG, "HTTP Response: ${response.code} ${response.message}")
            Log.d(TAG, "Full Raw API Response: $rawResponseBody")
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(debugInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    private lateinit var prefs: SharedPreferences
    private var isInitialized = false
    private var nextId = 1

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    fun initialize(context: Context) {
        if (isInitialized) return

        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedTasks = sortTasksForStorage(loadTasks())
        nextId = (storedTasks.maxOfOrNull(Task::id) ?: 0) + 1
        _tasks.value = storedTasks
        isInitialized = true
    }

    suspend fun extractTasksFromAI(notes: String): List<Task> {
        Log.d(TAG, "Starting Gemini task extraction for input: $notes")

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank()) {
            Log.e(TAG, "GEMINI_API_KEY is empty in BuildConfig")
            throw Exception("Gemini API key is missing. Check local.properties and rebuild the app.")
        }

        Log.d(TAG, "Configured Gemini base URL: $GEMINI_BASE_URL")
        Log.d(TAG, "Configured Gemini endpoint: ${GEMINI_BASE_URL}v1beta/{model}:generateContent")
        Log.d(TAG, "Configured Gemini model: $GEMINI_MODEL")

        val request = GeminiRequest.create(buildPrompt(notes))
        Log.d(TAG, "Request Payload: ${gson.toJson(request)}")

        return try {
            val response = requestGeminiWithRetry(
                model = GEMINI_MODEL,
                apiKey = apiKey,
                request = request
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                Log.e(TAG, "Gemini HTTP ${response.code()} error: $errorBody")
                throw Exception(buildApiErrorMessage(response.code(), errorBody))
            }

            val responseBody = response.body()
                ?: throw Exception("Gemini returned an empty response body.")

            Log.d(TAG, "Parsed Gemini response model: ${gson.toJson(responseBody)}")

            val responseText = responseBody.firstText()
            if (responseText.isNullOrBlank()) {
                val blockReason = responseBody.promptFeedback?.blockReason
                val message = if (blockReason.isNullOrBlank()) {
                    "Gemini returned no candidate text."
                } else {
                    "Gemini returned no candidate text. Block reason: $blockReason"
                }
                Log.e(TAG, message)
                throw Exception(message)
            }

            Log.d(TAG, "Raw response text: $responseText")

            val extractedTasks = parseExtractedTasks(responseText)
            val createdDate = TaskDateFormatter.today()
            val finalTasks = extractedTasks.mapNotNull { mapToTask(it, createdDate) }

            if (finalTasks.isEmpty()) {
                throw Exception("Gemini returned no usable tasks after parsing.")
            }

            Log.d(TAG, "Parsed output: ${gson.toJson(finalTasks)}")
            Log.d(TAG, "Extracted ${finalTasks.size} tasks end-to-end.")
            finalTasks
        } catch (error: Exception) {
            val errorMessage = error.message ?: "Unknown Gemini error"
            Log.e(TAG, "API FLOW EXCEPTION: $errorMessage", error)
            throw Exception(errorMessage)
        }
    }

    fun addTasks(tasksToAdd: List<Task>) {
        ensureInitialized()
        if (tasksToAdd.isEmpty()) return

        val createdTasks = tasksToAdd.map { task ->
            task.copy(id = nextId++)
        }
        persist(_tasks.value + createdTasks)
    }

    fun toggleTaskCompletion(taskId: Int) {
        ensureInitialized()
        val updatedTasks = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(isCompleted = !task.isCompleted)
            } else {
                task
            }
        }
        persist(updatedTasks)
    }

    fun getTasksByDate(date: String): List<Task> {
        ensureInitialized()
        return filterTasksByDate(_tasks.value, date)
    }

    internal fun sortTasksForStorage(tasks: List<Task>): List<Task> = tasks.sortedBy(Task::isCompleted)

    internal fun filterTasksByDate(tasks: List<Task>, date: String): List<Task> {
        return sortTasksForStorage(tasks.filter { task -> task.date == date })
    }

    private fun buildPrompt(notes: String): String = """
        Extract actionable tasks from the note below.
        Return every explicit task in the note.
        Use "High" for urgent or time-bound tasks, "Medium" for normal tasks, and "Low" for optional tasks.
        Use the exact due phrase if one exists. Otherwise use "Not specified".

        Note:
        $notes
    """.trimIndent()

    private suspend fun requestGeminiWithRetry(
        model: String,
        apiKey: String,
        request: GeminiRequest
    ): retrofit2.Response<GeminiResponse> {
        var attempt = 0

        while (true) {
            attempt += 1
            val response = apiService.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )

            if (response.isSuccessful) {
                return response
            }

            val code = response.code()
            val errorBody = response.errorBody()?.string().orEmpty()
            val isRetryable = code == 429 || code == 503

            Log.e(
                TAG,
                "Gemini HTTP $code on attempt $attempt/$MAX_GEMINI_RETRIES: $errorBody"
            )

            if (!isRetryable || attempt >= MAX_GEMINI_RETRIES) {
                throw Exception(buildApiErrorMessage(code, errorBody))
            }

            delay(attempt * 1_000L)
        }
    }

    private fun parseExtractedTasks(responseText: String): List<ExtractedTask> {
        val jsonPayload = extractJsonPayload(responseText)
        Log.d(TAG, "JSON payload for parsing: $jsonPayload")

        val jsonElement = try {
            JsonParser.parseString(jsonPayload)
        } catch (error: JsonSyntaxException) {
            Log.e(TAG, "Invalid JSON returned by Gemini after cleanup.", error)
            throw Exception("Gemini returned invalid JSON. Raw response: $responseText")
        }

        return when {
            jsonElement.isJsonArray -> gson.fromJson(jsonElement, extractedTaskListType)
            jsonElement.isJsonObject -> {
                val jsonObject = jsonElement.asJsonObject
                when {
                    jsonObject.has("tasks") && jsonObject.get("tasks").isJsonArray ->
                        gson.fromJson(jsonObject.get("tasks"), extractedTaskListType)
                    else -> listOf(gson.fromJson(jsonObject, ExtractedTask::class.java))
                }
            }
            else -> emptyList()
        }
    }

    private fun extractJsonPayload(responseText: String): String {
        val withoutCodeFences = responseText
            .trim()
            .replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

        if (withoutCodeFences.isBlank()) {
            return withoutCodeFences
        }

        runCatching {
            JsonParser.parseString(withoutCodeFences)
        }.onSuccess {
            return withoutCodeFences
        }

        return findBalancedJsonBlock(withoutCodeFences) ?: withoutCodeFences
    }

    private fun findBalancedJsonBlock(text: String): String? {
        var startIndex = -1
        val stack = ArrayDeque<Char>()
        var inString = false
        var isEscaped = false

        for (index in text.indices) {
            val character = text[index]

            if (startIndex == -1) {
                if (character == '[' || character == '{') {
                    startIndex = index
                    stack.addLast(character)
                }
                continue
            }

            if (inString) {
                when {
                    isEscaped -> isEscaped = false
                    character == '\\' -> isEscaped = true
                    character == '"' -> inString = false
                }
                continue
            }

            when (character) {
                '"' -> inString = true
                '{', '[' -> stack.addLast(character)
                '}' -> if (stack.lastOrNull() == '{') stack.removeLast()
                ']' -> if (stack.lastOrNull() == '[') stack.removeLast()
            }

            if (startIndex != -1 && stack.isEmpty()) {
                return text.substring(startIndex, index + 1).trim()
            }
        }

        return null
    }

    private fun mapToTask(extractedTask: ExtractedTask, createdDate: String): Task? {
        val taskName = extractedTask.task?.trim().orEmpty()
        if (taskName.isBlank()) {
            Log.e(TAG, "Skipping extracted task with blank title: ${gson.toJson(extractedTask)}")
            return null
        }

        return Task(
            id = 0,
            task = taskName,
            priority = validatePriority(extractedTask.priority),
            deadline = normalizeDeadline(extractedTask.deadline),
            date = createdDate,
            isCompleted = false
        )
    }

    private fun validatePriority(priority: String?): String {
        val normalized = priority
            ?.trim()
            ?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
            .orEmpty()

        return when (normalized) {
            "High", "Medium", "Low" -> normalized
            else -> "Medium"
        }
    }

    private fun normalizeDeadline(deadline: String?): String {
        val normalized = deadline?.trim().orEmpty()
        return normalized.ifBlank { "Not specified" }
    }

    private fun buildApiErrorMessage(code: Int, errorBody: String): String {
        val parsedError = runCatching {
            gson.fromJson(errorBody, GeminiErrorEnvelope::class.java)
        }.getOrNull()

        val message = parsedError?.error?.message?.takeIf { it.isNotBlank() }
            ?: errorBody.ifBlank { "Unknown Gemini error" }

        return "HTTP $code: $message"
    }

    private fun readRequestBody(body: RequestBody): String {
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }.getOrElse { error ->
            "<unable to read request body: ${error.message}>"
        }
    }

    private fun loadTasks(): List<Task> {
        val rawTasks = prefs.getString(KEY_TASKS, null).orEmpty()
        if (rawTasks.isBlank()) return emptyList()

        return runCatching {
            val jsonArray = JSONArray(rawTasks)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(index)
                    add(
                        Task(
                            id = item.optInt("id"),
                            task = item.optString("task"),
                            priority = item.optString("priority"),
                            deadline = item.optString("deadline"),
                            date = item.optString("date"),
                            isCompleted = item.optBoolean("isCompleted")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(updatedTasks: List<Task>) {
        val sortedTasks = sortTasksForStorage(updatedTasks)
        _tasks.value = sortedTasks
        val jsonArray = JSONArray()

        sortedTasks.forEach { task ->
            jsonArray.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("task", task.task)
                    put("priority", task.priority)
                    put("deadline", task.deadline)
                    put("date", task.date)
                    put("isCompleted", task.isCompleted)
                }
            )
        }

        prefs.edit().putString(KEY_TASKS, jsonArray.toString()).apply()
    }

    private fun ensureInitialized() {
        check(isInitialized) {
            "TaskRepository must be initialized before use."
        }
    }
}
