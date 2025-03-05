package com.pt.glancewords.data.di

import com.pt.glancewords.data.database.Database
import com.pt.glancewords.data.datasource.DefaultGoogleSpreadsheetDataSource
import com.pt.glancewords.data.datasource.FileWordsLocalDataSource
import com.pt.glancewords.data.datasource.GoogleWordsRemoteDataSource
import com.pt.glancewords.data.googlesheets.CachingGoogleSheetsProvider
import com.pt.glancewords.data.googlesheets.GoogleSheetsProvider
import com.pt.glancewords.data.mapper.DefaultSheetMapper
import com.pt.glancewords.data.mapper.DefaultWidgetMapper
import com.pt.glancewords.data.mapper.DefaultWordPairMapper
import com.pt.glancewords.data.repository.DefaultSheetRepository
import com.pt.glancewords.data.repository.DefaultWidgetRepository
import com.pt.glancewords.data.repository.DefaultWordsRepository
import com.pt.glancewords.data.repository.GoogleSpreadsheetRepository
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.SpreadsheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.AndroidClientEngine
import io.ktor.client.engine.android.AndroidEngineConfig
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.time.Instant

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
    factory<SheetRepository> { DefaultSheetRepository(get<Database>().dbSheetQueries, DefaultSheetMapper(Instant::now), Dispatchers.IO) }
    single<WordsRepository> {
        DefaultWordsRepository(
            GoogleWordsRemoteDataSource(get(), get()),
            FileWordsLocalDataSource(FileSystem.SYSTEM, get<File>(QUALIFIER_SPREADSHEETS_DIRECTORY).toOkioPath(), Dispatchers.IO),
            DefaultWordPairMapper()
        )
    }
    factory<WidgetRepository> { DefaultWidgetRepository(get<Database>().dbWidgetQueries, DefaultWidgetMapper(), Dispatchers.IO) }
    factory<SpreadsheetRepository> {
        GoogleSpreadsheetRepository(
            DefaultGoogleSpreadsheetDataSource(get(), get(), Dispatchers.IO)
        )
    }
}

val QUALIFIER_SPREADSHEETS_DIRECTORY = named("spreadsheets_directory")