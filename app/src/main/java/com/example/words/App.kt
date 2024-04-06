package com.example.words

import android.app.Application
import com.example.words.repository.SheetsProvider
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer
import com.example.words.settings.settingsDataStore
import kotlinx.coroutines.runBlocking

class App: Application(), DependencyContainer {
    private lateinit var wordsRepository: WordsRepository
    private lateinit var widgetSettingsRepository: WidgetSettingsRepository
    private lateinit var wordsSynchronizer: WordsSynchronizer
    private lateinit var spreadsheetRepository: SpreadsheetRepository

    override fun onCreate() {
        runBlocking { SheetsProvider.initialize { assets.open(it) } }
        wordsRepository = WordsRepository(filesDir)
        widgetSettingsRepository = WidgetSettingsRepository(settingsDataStore)
        wordsSynchronizer = WordsSynchronizer(wordsRepository, widgetSettingsRepository)
        spreadsheetRepository = SpreadsheetRepository(SheetsProvider.sheets)
        super.onCreate()
    }

    override fun getWordsRepository() = wordsRepository
    override fun getWidgetSettingsRepository() = widgetSettingsRepository
    override fun getWordsSynchronizer() = wordsSynchronizer
    override fun getSpreadsheetRepository() = spreadsheetRepository
}

interface DependencyContainer {
    fun getWordsRepository(): WordsRepository
    fun getWidgetSettingsRepository(): WidgetSettingsRepository
    fun getWordsSynchronizer(): WordsSynchronizer
    fun getSpreadsheetRepository(): SpreadsheetRepository
}