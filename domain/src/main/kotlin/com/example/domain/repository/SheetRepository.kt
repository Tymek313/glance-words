package com.example.domain.repository

import com.example.domain.model.Sheet
import com.example.domain.model.SheetId
import com.example.domain.model.SheetSpreadsheetId
import java.time.Instant

interface SheetRepository {
    suspend fun getSheets(): List<Sheet>
    suspend fun getBySheetSpreadsheetId(sheetSpreadsheetId: SheetSpreadsheetId): Sheet?
    suspend fun addSheet(sheet: Sheet): Sheet
    suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant)
}