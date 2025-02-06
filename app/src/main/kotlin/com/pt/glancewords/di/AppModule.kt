package com.pt.glancewords.di

import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.pt.glancewords.data.database.createDatabase
import com.pt.glancewords.data.di.QUALIFIER_SPREADSHEETS_DIRECTORY
import com.pt.glancewords.database.Database
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.synchronization.DefaultWordsSynchronizationStateNotifier
import com.pt.glancewords.domain.synchronization.DefaultWordsSynchronizer
import com.pt.glancewords.domain.synchronization.WordsSynchronizationStateNotifier
import com.pt.glancewords.domain.synchronization.WordsSynchronizer
import com.pt.glancewords.domain.usecase.AddWidget
import com.pt.glancewords.domain.usecase.DefaultAddWidget
import com.pt.glancewords.logging.DefaultLogger
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.widget.DefaultReshuffleNotifier
import com.pt.glancewords.widget.ReshuffleNotifier
import com.pt.glancewords.widget.WordsWidgetViewModel
import com.pt.glancewords.widget.configuration.WidgetConfigurationViewModel
import com.pt.glancewords.widget.refreshWidget
import com.pt.glancewords.work.SynchronizeWordsWorker
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
            WidgetId(GlanceAppWidgetManager(get()).getAppWidgetId(widgetId)),
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
    factory<AddWidget> { DefaultAddWidget(get(), get()) }
}