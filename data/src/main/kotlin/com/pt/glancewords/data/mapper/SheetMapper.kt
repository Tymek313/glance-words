package com.pt.glancewords.data.mapper

import com.pt.glancewords.data.database.DbSheet
import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import java.time.Instant

internal interface SheetMapper {
    fun mapToDomain(dbSheet: DbSheet): Sheet
    fun mapToDomain(newSheet: NewSheet, sheetId: SheetId): Sheet
    fun mapToDb(sheet: NewSheet): DbSheet
}

internal class DefaultSheetMapper(private val getNowInstant: () -> Instant) : SheetMapper {
    override fun mapToDomain(dbSheet: DbSheet): Sheet = with(dbSheet) {
        Sheet(
            id = SheetId(id),
            sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
            name = name,
            lastUpdatedAt = last_updated_at?.let(Instant::ofEpochSecond)
        )
    }

    override fun mapToDomain(newSheet: NewSheet, sheetId: SheetId): Sheet = with(newSheet) {
        Sheet(
            id = sheetId,
            sheetSpreadsheetId = sheetSpreadsheetId,
            name = name,
            lastUpdatedAt = null,
        )
    }

    override fun mapToDb(sheet: NewSheet): DbSheet = with(sheet) {
        DbSheet(
            id = -1,
            spreadsheet_id = sheetSpreadsheetId.spreadsheetId,
            sheet_id = sheetSpreadsheetId.sheetId,
            name = name,
            last_updated_at = getNowInstant().epochSecond
        )
    }
}
