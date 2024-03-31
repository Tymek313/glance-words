package com.example.words.repository

import com.example.words.model.SpreadsheetSheet
import com.google.api.services.sheets.v4.Sheets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpreadsheetRepository(
    private val sheets: Sheets,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet> = withContext(dispatcher) {
        sheets.spreadsheets().get(spreadsheetId).execute().sheets.map { sheet ->
            sheet.properties.run { SpreadsheetSheet(id = sheetId, name = title) }
        }
    }
}