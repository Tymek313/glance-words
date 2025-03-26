package com.pt.glancewords.data.di

import com.pt.glancewords.data.database.Database
import com.pt.glancewords.data.sheet.CachingGoogleSheetsProvider
import com.pt.glancewords.data.sheet.GoogleSheetsProvider
import com.pt.glancewords.data.sheet.datasource.DefaultGoogleSpreadsheetDataSource
import com.pt.glancewords.data.sheet.mapper.DefaultSheetMapper
import com.pt.glancewords.data.sheet.repository.DatabaseSheetRepository
import com.pt.glancewords.data.sheet.repository.GoogleSpreadsheetRepository
import com.pt.glancewords.data.widget.mapper.DefaultWidgetMapper
import com.pt.glancewords.data.widget.repository.DatabaseWidgetRepository
import com.pt.glancewords.data.words.datasource.DatabaseWordsLocalDataSource
import com.pt.glancewords.data.words.datasource.GoogleWordsRemoteDataSource
import com.pt.glancewords.data.words.mapper.CsvWordPairMapper
import com.pt.glancewords.data.words.mapper.DefaultCsvWordPairMapper
import com.pt.glancewords.data.words.repository.DefaultWordsRepository
import com.pt.glancewords.domain.sheet.repository.SheetRepository
import com.pt.glancewords.domain.sheet.repository.SpreadsheetRepository
import com.pt.glancewords.domain.widget.repository.WidgetRepository
import com.pt.glancewords.domain.words.repository.WordsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.AndroidClientEngine
import io.ktor.client.engine.android.AndroidEngineConfig
import io.ktor.client.plugins.HttpTimeout
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val dataModule = module {
    single<GoogleSheetsProvider> { CachingGoogleSheetsProvider(Dispatchers.IO) }
    single {
        HttpClient(AndroidClientEngine(AndroidEngineConfig())) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15000
                requestTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }
        }
    }
    factory<SheetRepository> { DatabaseSheetRepository(get<Database>().dbSheetQueries, DefaultSheetMapper(Instant::now), Dispatchers.IO) }
    factoryOf<CsvWordPairMapper>(::DefaultCsvWordPairMapper)
    single<WordsRepository> {
        DefaultWordsRepository(
            GoogleWordsRemoteDataSource(get(), get(), get()),
            DatabaseWordsLocalDataSource(get<Database>().dbWordPairQueries, Dispatchers.IO)
        )
    }
    factory<WidgetRepository> { DatabaseWidgetRepository(get<Database>().dbWidgetQueries, DefaultWidgetMapper(), Dispatchers.IO) }
    factory<SpreadsheetRepository> {
        GoogleSpreadsheetRepository(
            DefaultGoogleSpreadsheetDataSource(get(), get(), Dispatchers.IO)
        )
    }
}
