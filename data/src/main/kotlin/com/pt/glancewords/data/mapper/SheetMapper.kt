package com.pt.glancewords.data.mapper

import com.pt.glancewords.database.DbSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import java.time.Instant

internal interface SheetMapper {
    fun mapToDomain(dbSheet: DbSheet): Sheet
    fun mapToDb(sheet: Sheet): DbSheet
}

internal class DefaultSheetMapper : SheetMapper {
    override fun mapToDomain(dbSheet: DbSheet): Sheet = with(dbSheet) {
        Sheet.createExisting(
            id = SheetId(id),
            sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
            name = name,
            lastUpdatedAt = last_updated_at?.let(Instant::ofEpochSecond)
        )
    }

    override fun mapToDb(sheet: Sheet): DbSheet = with(sheet) {
        DbSheet(
            id = id.value,
            spreadsheet_id = sheetSpreadsheetId.spreadsheetId,
            sheet_id = sheetSpreadsheetId.sheetId,
            name = name,
            last_updated_at = lastUpdatedAt?.epochSecond
        )
    }
}
