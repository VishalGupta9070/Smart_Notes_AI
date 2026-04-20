package com.example.smartnotesai

import android.app.Application
import com.example.smartnotesai.data.repository.TaskRepository

class SmartNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TaskRepository.initialize(this)
    }
}
