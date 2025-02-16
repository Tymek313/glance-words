package com.pt.glancewords.domain.repository

import com.pt.glancewords.domain.model.SpreadsheetSheet

interface SpreadsheetRepository {
    suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet>?
}