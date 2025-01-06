package com.example.words.mapper

import com.example.words.database.DbSheet
import com.example.words.model.Sheet
import com.example.words.model.SheetId
import com.example.words.model.SheetSpreadsheetId
import java.time.Instant

interface SheetMapper {
    fun mapToDomain(dbSheet: DbSheet): Sheet
    fun mapToDb(sheet: Sheet): DbSheet
}

class DefaultSheetMapper: SheetMapper {
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
