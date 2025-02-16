package com.pt.glancewords.data.repository

import com.pt.glancewords.data.datasource.GoogleSpreadsheetDataSource
import com.pt.glancewords.domain.model.SpreadsheetSheet
import com.pt.glancewords.domain.repository.SpreadsheetRepository

internal class GoogleSpreadsheetRepository(private val dataSource: GoogleSpreadsheetDataSource) : SpreadsheetRepository {
    override suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet>? {
        return dataSource.getSpreadsheets(spreadsheetId)?.map { sheet ->
            sheet.properties.run { SpreadsheetSheet(id = sheetId, name = title) }
        }
    }
}