package com.example.words.datasource

import com.example.words.googlesheets.GoogleSheetsProvider
import com.google.api.services.sheets.v4.model.Sheet

interface GoogleSpreadsheetDataSource {
    suspend fun getSpreadsheets(id: String): List<Sheet>?
}

class DefaultGoogleSpreadsheetDataSource(
    private val sheetsProvider: GoogleSheetsProvider
) : GoogleSpreadsheetDataSource {
    override suspend fun getSpreadsheets(id: String): List<Sheet>? {
        return sheetsProvider.getGoogleSheets().spreadsheets().get(id).execute().sheets
    }
}