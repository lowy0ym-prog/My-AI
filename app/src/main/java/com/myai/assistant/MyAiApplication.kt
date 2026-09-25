package com.myai.assistant

import android.app.Application
import com.myai.assistant.data.db.AppDatabase

class MyAiApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
