package com.pt.glancewords.domain.sheet.repository

import com.pt.glancewords.domain.sheet.model.SpreadsheetSheet

interface SpreadsheetRepository {
    suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet>?
}
