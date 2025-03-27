package com.pt.glancewords.domain.sheet.repository

import com.pt.glancewords.domain.sheet.model.NewSheet
import com.pt.glancewords.domain.sheet.model.Sheet
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.sheet.model.SheetSpreadsheetId
import java.time.Instant

interface SheetRepository {
    suspend fun getBySheetSpreadsheetId(sheetSpreadsheetId: SheetSpreadsheetId): Sheet?
    suspend fun addSheet(sheet: NewSheet): Sheet
    suspend fun deleteSheet(sheetId: SheetId)
    suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant)
}
