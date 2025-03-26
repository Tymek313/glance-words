package com.pt.glancewords.data.sheet.repository

import com.pt.glancewords.data.sheet.datasource.GoogleSpreadsheetDataSource
import com.pt.glancewords.domain.sheet.model.SpreadsheetSheet
import com.pt.glancewords.domain.sheet.repository.SpreadsheetRepository

internal class GoogleSpreadsheetRepository(private val dataSource: GoogleSpreadsheetDataSource) : SpreadsheetRepository {
    override suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet>? =
        dataSource.getSpreadsheets(spreadsheetId)?.map { sheet ->
            sheet.properties.run { SpreadsheetSheet(id = sheetId, name = title) }
        }
}
