package com.example.words.data.repository

import com.example.domain.model.SpreadsheetSheet
import com.example.domain.repository.SpreadsheetRepository
import com.example.words.data.datasource.GoogleSpreadsheetDataSource

internal class GoogleSpreadsheetRepository(private val dataSource: GoogleSpreadsheetDataSource) : SpreadsheetRepository {
    override suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet> {
        return dataSource.getSpreadsheets(spreadsheetId).map { sheet ->
            sheet.properties.run { SpreadsheetSheet(id = sheetId, name = title) }
        }
    }
}