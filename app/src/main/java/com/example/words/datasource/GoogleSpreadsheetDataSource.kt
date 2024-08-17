package com.example.words.datasource

import com.example.words.googlesheets.GoogleSheetsProvider
import com.google.api.services.sheets.v4.model.Sheet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface GoogleSpreadsheetDataSource {
    suspend fun getSpreadsheets(id: String): List<Sheet>
}

class DefaultGoogleSpreadsheetDataSource(
    private val sheetsProvider: GoogleSheetsProvider,
    private val ioDispatcher: CoroutineDispatcher
) : GoogleSpreadsheetDataSource {
    override suspend fun getSpreadsheets(id: String): List<Sheet> = withContext(ioDispatcher) {
        sheetsProvider.getGoogleSheets().spreadsheets().get(id).execute().sheets ?: emptyList()
    }
}