package com.example.words.repository

import com.example.words.datasource.GoogleSpreadsheetDataSource
import com.example.words.model.SpreadsheetSheet

interface SpreadsheetRepository {
    suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet>
}

class GoogleSpreadsheetRepository(private val dataSource: GoogleSpreadsheetDataSource) : SpreadsheetRepository {
    override suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet> {
        return dataSource.getSpreadsheets(spreadsheetId).map { sheet ->
            sheet.properties.run { SpreadsheetSheet(id = sheetId, name = title) }
        }
    }
}