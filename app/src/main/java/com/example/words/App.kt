package com.example.words

import android.app.Application
import com.example.words.logging.DefaultLogger
import com.example.words.logging.Logger
import com.example.words.repository.DefaultSpreadsheetRepository
import com.example.words.repository.DefaultWidgetSettingsRepository
import com.example.words.repository.DefaultWordsRepository
import com.example.words.repository.DefaultWordsSynchronizer
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
    private lateinit var logger: Logger

    override fun onCreate() {
        runBlocking { SheetsProvider.initialize { assets.open(it) } }
        wordsRepository = DefaultWordsRepository(filesDir)
        widgetSettingsRepository = DefaultWidgetSettingsRepository(settingsDataStore)
        wordsSynchronizer = DefaultWordsSynchronizer(wordsRepository, widgetSettingsRepository)
        spreadsheetRepository = DefaultSpreadsheetRepository(SheetsProvider.sheets)
        logger = DefaultLogger()
        super.onCreate()
    }

    override fun getWordsRepository() = wordsRepository
    override fun getWidgetSettingsRepository() = widgetSettingsRepository
    override fun getWordsSynchronizer() = wordsSynchronizer
    override fun getSpreadsheetRepository() = spreadsheetRepository
    override fun getLogger() = logger
}

interface DependencyContainer {
    fun getWordsRepository(): WordsRepository
    fun getWidgetSettingsRepository(): WidgetSettingsRepository
    fun getWordsSynchronizer(): WordsSynchronizer
    fun getSpreadsheetRepository(): SpreadsheetRepository
    fun getLogger(): Logger
}