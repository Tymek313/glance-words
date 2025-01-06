package com.example.words.di

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.words.database.Database
import com.example.words.database.createDatabase
import com.example.words.datasource.DefaultGoogleSpreadsheetDataSource
import com.example.words.datasource.FileWordsLocalDataSource
import com.example.words.datasource.GoogleWordsRemoteDataSource
import com.example.words.googlesheets.CachingGoogleSheetsProvider
import com.example.words.logging.DefaultLogger
import com.example.words.logging.Logger
import com.example.words.mapper.DefaultSheetMapper
import com.example.words.mapper.DefaultWordPairMapper
import com.example.words.model.Widget
import com.example.words.repository.DefaultSheetRepository
import com.example.words.repository.DefaultWidgetRepository
import com.example.words.repository.DefaultWordsRepository
import com.example.words.repository.GoogleSpreadsheetRepository
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import com.example.words.synchronization.DefaultWordsSynchronizationStateNotifier
import com.example.words.synchronization.DefaultWordsSynchronizer
import com.example.words.synchronization.WordsSynchronizationStateNotifier
import com.example.words.synchronization.WordsSynchronizer
import com.example.words.widget.DefaultReshuffleNotifier
import com.example.words.widget.ReshuffleNotifier
import com.example.words.widget.WordsWidgetViewModel
import com.example.words.widget.refreshWidget
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.AndroidClientEngine
import io.ktor.client.engine.android.AndroidEngineConfig
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

interface DependencyContainer {
    val wordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier
    val wordsRepository: WordsRepository
    val widgetRepository: WidgetRepository
    val wordsSynchronizer: WordsSynchronizer
    val spreadsheetRepository: SpreadsheetRepository
    val logger: Logger
    val reshuffleNotifier: ReshuffleNotifier
    fun getWordsWidgetViewModel(widgetId: GlanceId): WordsWidgetViewModel
}

class DefaultDependencyContainer : DependencyContainer {

    private lateinit var appContext: Context

    private lateinit var database: Database

    private val googleSheetsProvider by lazy { CachingGoogleSheetsProvider(appContext.resources, Dispatchers.IO) }

    override val wordsSynchronizationStateNotifier by lazy { DefaultWordsSynchronizationStateNotifier() }

    private val httpClient by lazy { HttpClient(AndroidClientEngine(AndroidEngineConfig())) }

    override val logger by lazy { DefaultLogger() }

    private val sheetRepository
        get() = DefaultSheetRepository(database.dbSheetQueries, DefaultSheetMapper(), Dispatchers.IO)

    override val wordsRepository by lazy {
        DefaultWordsRepository(
            GoogleWordsRemoteDataSource(httpClient),
            FileWordsLocalDataSource(FileSystem.SYSTEM, appContext.filesDir.toOkioPath(), Dispatchers.IO),
            DefaultWordPairMapper()
        )
    }

    override val widgetRepository
        get() = DefaultWidgetRepository(database.dbWidgetQueries, sheetRepository, Dispatchers.IO)

    override val wordsSynchronizer
        get() = DefaultWordsSynchronizer(
            wordsRepository,
            widgetRepository,
            sheetRepository,
            wordsSynchronizationStateNotifier,
            refreshWidget = { refreshWidget(context = appContext, widgetId = it) },
            Instant::now
        )

    override val spreadsheetRepository
        get() = GoogleSpreadsheetRepository(
            DefaultGoogleSpreadsheetDataSource(googleSheetsProvider, Dispatchers.IO)
        )

    override val reshuffleNotifier by lazy { DefaultReshuffleNotifier() }

    fun initialize(appContext: Context) {
        this.appContext = appContext
        database = createDatabase(
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = appContext,
                name = "database.db",
                callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                }
            )
        )
    }

    override fun getWordsWidgetViewModel(widgetId: GlanceId): WordsWidgetViewModel = WordsWidgetViewModel(
        Widget.WidgetId(GlanceAppWidgetManager(appContext).getAppWidgetId(widgetId)),
        widgetRepository,
        wordsRepository,
        wordsSynchronizationStateNotifier,
        logger,
        Locale.getDefault(),
        ZoneId.systemDefault(),
        reshuffleNotifier
    )
}