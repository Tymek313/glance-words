package com.example.words.repository

import com.example.words.database.DbSheet
import com.example.words.database.DbSheetQueries
import com.example.words.model.Sheet
import com.example.words.model.SheetId
import com.example.words.model.SheetSpreadsheetId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant

interface SheetRepository {
    suspend fun getSheets(): List<Sheet>
    suspend fun addSheet(sheet: Sheet): Sheet
    suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant)
}

class DefaultSheetRepository(
    private val database: DbSheetQueries,
    private val ioDispatcher: CoroutineDispatcher
) : SheetRepository {

    override suspend fun getSheets(): List<Sheet> = withContext(ioDispatcher) {
        database.getAll().executeAsList().map { it.toDomain() }
    }

    private fun DbSheet.toDomain() = Sheet(
        id = SheetId(id),
        sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
        name = name,
        lastUpdatedAt = last_updated_at?.let(Instant::ofEpochSecond)
    )

    override suspend fun addSheet(sheet: Sheet) = withContext(ioDispatcher) {
        val sheetId = database.transactionWithResult {
            database.insert(sheet.toDb())
            database.getLastId().executeAsOne().toInt()
        }
        sheet.copy(id = SheetId(sheetId))
    }

    private fun Sheet.toDb() = DbSheet(
        id = id.value,
        spreadsheet_id = sheetSpreadsheetId.spreadsheetId,
        sheet_id = sheetSpreadsheetId.sheetId,
        name = name,
        last_updated_at = lastUpdatedAt?.epochSecond
    )

    override suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant) = withContext(ioDispatcher) {
        database.updateLastUpdatedAt(id = sheetId.value, last_updated_at = lastUpdatedAt.epochSecond)
    }
}