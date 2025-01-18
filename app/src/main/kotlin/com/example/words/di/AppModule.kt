package com.example.words.di

import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.domain.model.Widget
import com.example.domain.synchronization.DefaultWordsSynchronizationStateNotifier
import com.example.domain.synchronization.DefaultWordsSynchronizer
import com.example.domain.synchronization.WordsSynchronizationStateNotifier
import com.example.domain.synchronization.WordsSynchronizer
import com.example.words.data.database.createDatabase
import com.example.words.data.di.QUALIFIER_SPREADSHEETS_DIRECTORY
import com.example.words.database.Database
import com.example.words.logging.DefaultLogger
import com.example.words.logging.Logger
import com.example.words.widget.DefaultReshuffleNotifier
import com.example.words.widget.ReshuffleNotifier
import com.example.words.widget.WordsWidgetViewModel
import com.example.words.widget.configuration.WidgetConfigurationViewModel
import com.example.words.widget.refreshWidget
import com.example.words.work.SynchronizeWordsWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

val appModule = module {
    singleOf<ReshuffleNotifier>(::DefaultReshuffleNotifier)
    factory { (widgetId: GlanceId) ->
        WordsWidgetViewModel(
            Widget.WidgetId(GlanceAppWidgetManager(get()).getAppWidgetId(widgetId)),
            get(),
            get(),
            get(),
            get(),
            Locale.getDefault(),
            ZoneId.systemDefault(),
            get()
        )
    }
    singleOf<WordsSynchronizationStateNotifier>(::DefaultWordsSynchronizationStateNotifier)
    single<WordsSynchronizer> {
        DefaultWordsSynchronizer(
            get(),
            get(),
            get(),
            get(),
            refreshWidget = { refreshWidget(context = androidContext(), widgetId = it) },
            Instant::now
        )
    }
    singleOf<Logger>(::DefaultLogger)
    workerOf(::SynchronizeWordsWorker)
    viewModelOf(::WidgetConfigurationViewModel)
    single {
        createDatabase(
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = androidContext(),
                name = "database.db",
                callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                }
            )
        )
    }
    factory(QUALIFIER_SPREADSHEETS_DIRECTORY) { androidContext().filesDir }
}