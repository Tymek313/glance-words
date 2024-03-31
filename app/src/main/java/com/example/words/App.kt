package com.example.words

import android.app.Application
import com.example.words.repository.SheetsProvider
import kotlinx.coroutines.runBlocking

class App: Application() {
    override fun onCreate() {
        runBlocking { SheetsProvider.initialize { assets.open(it) } }
        super.onCreate()
    }
}