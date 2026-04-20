package com.example.smartnotesai.data.repository

import android.content.SharedPreferences
import com.example.smartnotesai.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TaskRepositoryBehaviorTest {
    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        setPrivateField("prefs", fakePrefs)
        setPrivateField("isInitialized", true)
        setPrivateField("nextId", 100)
        setTaskState(emptyList())
    }

    @After
    fun tearDown() {
        setPrivateField("isInitialized", false)
        setPrivateField("nextId", 1)
        setTaskState(emptyList())
    }

    @Test
    fun completedTasksMoveToBottomImmediatelyAfterToggle() {
        setTaskState(
            listOf(
                task(id = 1, title = "Call client", date = "20 Apr 2026"),
                task(id = 2, title = "Finish UI", date = "20 Apr 2026"),
                task(id = 3, title = "Prepare notes", date = "20 Apr 2026")
            )
        )

        TaskRepository.toggleTaskCompletion(2)

        val updatedTasks = TaskRepository.tasks.value
        assertEquals(listOf(1, 3, 2), updatedTasks.map(Task::id))
        assertEquals(listOf(false, false, true), updatedTasks.map(Task::isCompleted))
    }

    @Test
    fun getTasksByDateReturnsOnlyMatchingTasksWithCompletedItemsLast() {
        setTaskState(
            listOf(
                task(id = 1, title = "Call client", date = "20 Apr 2026"),
                task(id = 2, title = "Pay invoice", date = "21 Apr 2026"),
                task(id = 3, title = "Finish UI", date = "20 Apr 2026", isCompleted = true),
                task(id = 4, title = "Review docs", date = "20 Apr 2026")
            )
        )

        val filteredTasks = TaskRepository.getTasksByDate("20 Apr 2026")

        assertEquals(listOf(1, 4, 3), filteredTasks.map(Task::id))
        assertEquals(listOf("20 Apr 2026", "20 Apr 2026", "20 Apr 2026"), filteredTasks.map(Task::date))
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

    private fun setTaskState(tasks: List<Task>) {
        @Suppress("UNCHECKED_CAST")
        val stateFlow = getPrivateField("_tasks") as MutableStateFlow<List<Task>>
        stateFlow.value = TaskRepository.sortTasksForStorage(tasks)
    }

    private fun setPrivateField(name: String, value: Any) {
        val field = TaskRepository::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(TaskRepository, value)
    }

    private fun getPrivateField(name: String): Any {
        val field = TaskRepository::class.java.getDeclaredField(name)
        field.isAccessible = true
        return requireNotNull(field.get(TaskRepository))
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            return values[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return values[key] as? MutableSet<String> ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class Editor(
            private val values: LinkedHashMap<String, Any?>
        ) : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var shouldClear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                key?.let { pending[it] = value }
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                key?.let { pending[it] = values }
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                key?.let { pending[it] = value }
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                key?.let { pending[it] = value }
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                key?.let { pending[it] = value }
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                key?.let { pending[it] = value }
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                key?.let { removals += it }
            }

            override fun clear(): SharedPreferences.Editor = apply {
                shouldClear = true
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (shouldClear) {
                    values.clear()
                }
                removals.forEach(values::remove)
                values.putAll(pending)
            }
        }
    }
}
