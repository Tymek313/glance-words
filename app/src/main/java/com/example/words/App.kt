package com.example.words

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.glancewords.R
import com.example.words.datasource.DefaultGoogleSpreadsheetDataSource
import com.example.words.datasource.FileWordsLocalDataSource
import com.example.words.datasource.GoogleWordsRemoteDataSource
import com.example.words.googlesheets.CachingGoogleSheetsProvider
import com.example.words.logging.DefaultLogger
import com.example.words.logging.Logger
import com.example.words.mapper.DefaultWordPairMapper
import com.example.words.notification.NotificationChannels
import com.example.words.persistence.DataStorePersistence
import com.example.words.repository.DefaultWidgetLoadingStateSynchronizer
import com.example.words.repository.DefaultWidgetSettingsRepository
import com.example.words.repository.DefaultWordsRepository
import com.example.words.repository.DefaultWordsSynchronizer
import com.example.words.repository.GoogleSpreadsheetRepository
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetLoadingStateSynchronizer
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer
import com.example.words.settings.settingsDataStore
import com.example.words.work.WorkFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.AndroidClientEngine
import io.ktor.client.engine.android.AndroidEngineConfig
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.time.Instant

class App : Application(), DependencyContainer {
    override lateinit var wordsRepository: WordsRepository
    override lateinit var widgetSettingsRepository: WidgetSettingsRepository
    override lateinit var wordsSynchronizer: WordsSynchronizer
    override lateinit var spreadsheetRepository: SpreadsheetRepository
    override lateinit var logger: Logger
    override lateinit var widgetLoadingStateSynchronizer: WidgetLoadingStateSynchronizer

    override fun onCreate() {
        createDependencies()
        createNotificationChannels()
        initializeWorkManager()
        super.onCreate()
    }

    private fun createNotificationChannels() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannels.WIDGET_SYNCHRONIZATION.createChannel(::getString)
        )
    }

    private fun initializeWorkManager() {
        val configuration = Configuration.Builder()
            .setWorkerFactory(WorkFactory(wordsSynchronizer, logger, widgetLoadingStateSynchronizer))
            .build()
        WorkManager.initialize(this, configuration)
    }

    private fun createDependencies() {
        wordsRepository = DefaultWordsRepository(
            GoogleWordsRemoteDataSource(HttpClient(AndroidClientEngine(AndroidEngineConfig()))),
            FileWordsLocalDataSource(FileSystem.SYSTEM, filesDir.toOkioPath(), Dispatchers.IO),
            DefaultWordPairMapper()
        )
        widgetSettingsRepository = DefaultWidgetSettingsRepository(DataStorePersistence(settingsDataStore))
        wordsSynchronizer = DefaultWordsSynchronizer(wordsRepository, widgetSettingsRepository, Instant::now)
        spreadsheetRepository = GoogleSpreadsheetRepository(
            DefaultGoogleSpreadsheetDataSource(
                CachingGoogleSheetsProvider(Dispatchers.IO) { resources.openRawResource(R.raw.google_sheets_credentials) },
                Dispatchers.IO
            )
        )
        logger = DefaultLogger()
        widgetLoadingStateSynchronizer = DefaultWidgetLoadingStateSynchronizer()
    }
}