package dev.solora

import android.app.Application
import dev.solora.data.AppDatabase

class SoloraApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
    }
}


