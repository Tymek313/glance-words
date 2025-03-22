package com.pt.glancewords.domain.repository

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import java.time.Instant

interface SheetRepository {
    suspend fun getSheets(): List<Sheet>
    suspend fun getBySheetSpreadsheetId(sheetSpreadsheetId: SheetSpreadsheetId): Sheet?
    suspend fun exists(sheetId: SheetId): Boolean
    suspend fun addSheet(sheet: NewSheet): Sheet
    suspend fun deleteSheet(sheetId: SheetId)
    suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant)
}
