package com.example.words.data.datasource

import com.example.words.data.googlesheets.GoogleSheetsProvider
import com.google.api.services.sheets.v4.model.Sheet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface GoogleSpreadsheetDataSource {
    suspend fun getSpreadsheets(id: String): List<Sheet>
}

internal class DefaultGoogleSpreadsheetDataSource(
    private val sheetsProvider: GoogleSheetsProvider,
    private val ioDispatcher: CoroutineDispatcher
) : GoogleSpreadsheetDataSource {
    override suspend fun getSpreadsheets(id: String): List<Sheet> = withContext(ioDispatcher) {
        sheetsProvider.getGoogleSheets().spreadsheets().get(id).execute().sheets ?: emptyList()
    }
}