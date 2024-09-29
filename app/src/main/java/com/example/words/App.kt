package com.example.words

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Configuration
import androidx.work.WorkManager
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.words.database.Database
import com.example.words.database.createDatabase
import com.example.words.datasource.DefaultGoogleSpreadsheetDataSource
import com.example.words.datasource.FileWordsLocalDataSource
import com.example.words.datasource.GoogleWordsRemoteDataSource
import com.example.words.googlesheets.CachingGoogleSheetsProvider
import com.example.words.logging.DefaultLogger
import com.example.words.logging.Logger
import com.example.words.mapper.DefaultWordPairMapper
import com.example.words.notification.NotificationChannel
import com.example.words.repository.DefaultSheetRepository
import com.example.words.repository.DefaultWidgetLoadingStateNotifier
import com.example.words.repository.DefaultWidgetRepository
import com.example.words.repository.DefaultWordsRepository
import com.example.words.repository.DefaultWordsSynchronizer
import com.example.words.repository.GoogleSpreadsheetRepository
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetLoadingStateNotifier
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer
import com.example.words.widget.refreshWidget
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
    override lateinit var widgetRepository: WidgetRepository
    override lateinit var wordsSynchronizer: WordsSynchronizer
    override lateinit var spreadsheetRepository: SpreadsheetRepository
    override lateinit var logger: Logger
    override lateinit var widgetLoadingStateNotifier: WidgetLoadingStateNotifier

    override fun onCreate() {
        createDependencies()
        createNotificationChannels()
        initializeWorkManager()
        super.onCreate()
    }

    private fun createNotificationChannels() {
        val notificationManager = NotificationManagerCompat.from(this)
        NotificationChannel.entries.forEach { channel ->
            notificationManager.createNotificationChannel(
                channel.createAndroidChannel(::getString)
            )
        }
    }

    private fun initializeWorkManager() {
        val configuration = Configuration.Builder()
            .setWorkerFactory(WorkFactory(wordsSynchronizer, logger))
            .build()
        WorkManager.initialize(this, configuration)
    }

    private fun createDependencies() {
        val database = createDatabase(
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = this,
                name = "database.db",
                callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                }
            )
        )
        val sheetRepository = DefaultSheetRepository(database.dbSheetQueries, Dispatchers.IO)
        wordsRepository = DefaultWordsRepository(
            GoogleWordsRemoteDataSource(HttpClient(AndroidClientEngine(AndroidEngineConfig()))),
            FileWordsLocalDataSource(FileSystem.SYSTEM, filesDir.toOkioPath(), Dispatchers.IO),
            DefaultWordPairMapper()
        )
        widgetRepository = DefaultWidgetRepository(database.dbWidgetQueries, sheetRepository, Dispatchers.IO)
        widgetLoadingStateNotifier = DefaultWidgetLoadingStateNotifier()
        wordsSynchronizer = DefaultWordsSynchronizer(
            wordsRepository,
            widgetRepository,
            sheetRepository,
            widgetLoadingStateNotifier,
            refreshWidget = { refreshWidget(context = this, widgetId = it) },
            Instant::now
        )
        spreadsheetRepository = GoogleSpreadsheetRepository(
            DefaultGoogleSpreadsheetDataSource(
                CachingGoogleSheetsProvider(resources, Dispatchers.IO),
                Dispatchers.IO
            )
        )
        logger = DefaultLogger()
    }
}