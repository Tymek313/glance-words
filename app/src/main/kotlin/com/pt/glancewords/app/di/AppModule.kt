package com.pt.glancewords.app.di

import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.pt.glancewords.app.logging.DefaultLogger
import com.pt.glancewords.app.widget.DefaultReshuffleNotifier
import com.pt.glancewords.app.widget.ReshuffleNotifier
import com.pt.glancewords.app.widget.WordsWidgetViewModel
import com.pt.glancewords.app.widget.configuration.WidgetConfigurationViewModel
import com.pt.glancewords.app.widget.refreshWidget
import com.pt.glancewords.app.work.SynchronizeWordsWorker
import com.pt.glancewords.data.database.Database
import com.pt.glancewords.data.database.createDatabase
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.domain.widget.usecase.AddWidget
import com.pt.glancewords.domain.widget.usecase.DefaultAddWidget
import com.pt.glancewords.domain.words.synchronization.DefaultWordsSynchronizationStateNotifier
import com.pt.glancewords.domain.words.synchronization.WordsSynchronizationStateNotifier
import com.pt.glancewords.domain.words.usecase.DefaultSynchronizeWords
import com.pt.glancewords.domain.words.usecase.SynchronizeWords
import com.pt.glancewords.logging.Logger
import java.time.Instant
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf<ReshuffleNotifier>(::DefaultReshuffleNotifier)
    factory { (widgetId: GlanceId) ->
        WordsWidgetViewModel(
            WidgetId(GlanceAppWidgetManager(get()).getAppWidgetId(widgetId)),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    singleOf<WordsSynchronizationStateNotifier>(::DefaultWordsSynchronizationStateNotifier)
    single<SynchronizeWords> {
        DefaultSynchronizeWords(
            get(),
            get(),
            get(),
            get(),
            refreshWidget = { refreshWidget(context = androidContext(), widgetId = it) },
            logger = get(),
            getNowInstant = Instant::now
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
    factory<AddWidget> { DefaultAddWidget(get(), get(), get(), get()) }
}
