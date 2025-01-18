package com.example.words.data.di


import com.example.domain.repository.SheetRepository
import com.example.domain.repository.SpreadsheetRepository
import com.example.domain.repository.WidgetRepository
import com.example.domain.repository.WordsRepository
import com.example.words.data.datasource.DefaultGoogleSpreadsheetDataSource
import com.example.words.data.datasource.FileWordsLocalDataSource
import com.example.words.data.datasource.GoogleWordsRemoteDataSource
import com.example.words.data.googlesheets.CachingGoogleSheetsProvider
import com.example.words.data.googlesheets.GoogleSheetsProvider
import com.example.words.data.mapper.DefaultSheetMapper
import com.example.words.data.mapper.DefaultWordPairMapper
import com.example.words.data.repository.DefaultSheetRepository
import com.example.words.data.repository.DefaultWidgetRepository
import com.example.words.data.repository.DefaultWordsRepository
import com.example.words.data.repository.GoogleSpreadsheetRepository
import com.example.words.database.Database
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.AndroidClientEngine
import io.ktor.client.engine.android.AndroidEngineConfig
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

val dataModule = module {
    single<GoogleSheetsProvider> { CachingGoogleSheetsProvider(Dispatchers.IO) }
    single { HttpClient(AndroidClientEngine(AndroidEngineConfig())) }
    factory<SheetRepository> { DefaultSheetRepository(get<Database>().dbSheetQueries, DefaultSheetMapper(), Dispatchers.IO) }
    single<WordsRepository> {
        DefaultWordsRepository(
            GoogleWordsRemoteDataSource(get()),
            FileWordsLocalDataSource(FileSystem.SYSTEM, get<File>(QUALIFIER_SPREADSHEETS_DIRECTORY).toOkioPath(), Dispatchers.IO),
            DefaultWordPairMapper()
        )
    }
    factory<WidgetRepository> { DefaultWidgetRepository(get<Database>().dbWidgetQueries, get(), Dispatchers.IO) }
    factory<SpreadsheetRepository> {
        GoogleSpreadsheetRepository(
            DefaultGoogleSpreadsheetDataSource(get(), Dispatchers.IO)
        )
    }
}

val QUALIFIER_SPREADSHEETS_DIRECTORY = named("spreadsheets_directory")

