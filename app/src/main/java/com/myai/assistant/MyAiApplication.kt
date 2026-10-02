package com.myai.assistant

import android.app.Application
import com.myai.assistant.data.db.AppDatabase
import com.myai.assistant.inference.EngineHolder

class MyAiApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    // Shared across the whole app so the Models screen and the Chat screen
    // see the same loaded model instead of each holding its own engine.
    val engineHolder: EngineHolder by lazy { EngineHolder() }

    override fun onCreate() {
        super.onCreate()
    }
}
