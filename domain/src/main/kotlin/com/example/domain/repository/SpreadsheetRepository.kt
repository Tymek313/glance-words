package com.example.domain.repository

import com.example.domain.model.SpreadsheetSheet

interface SpreadsheetRepository {
    suspend fun fetchSpreadsheetSheets(spreadsheetId: String): List<SpreadsheetSheet>
}