package com.example.words.repository

import com.example.words.database.DbSheetQueries
import com.example.words.mapper.SheetMapper
import com.example.words.model.Sheet
import com.example.words.model.SheetId
import com.example.words.model.SheetSpreadsheetId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant

interface SheetRepository {
    suspend fun getSheets(): List<Sheet>
    suspend fun getBySheetSpreadsheetId(sheetSpreadsheetId: SheetSpreadsheetId): Sheet?
    suspend fun addSheet(sheet: Sheet): Sheet
    suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant)
}

class DefaultSheetRepository(
    private val database: DbSheetQueries,
    private val sheetMapper: SheetMapper,
    private val ioDispatcher: CoroutineDispatcher
) : SheetRepository {

    override suspend fun getSheets(): List<Sheet> = withContext(ioDispatcher) {
        database.getAll().executeAsList().map(sheetMapper::mapToDomain)
    }

    override suspend fun addSheet(sheet: Sheet) = withContext(ioDispatcher) {
        val sheetId = database.transactionWithResult {
            database.insert(sheetMapper.mapToDb(sheet))
            database.getLastId().executeAsOne().toInt()
        }
        sheet.copy(id = SheetId(sheetId))
    }

    override suspend fun getBySheetSpreadsheetId(sheetSpreadsheetId: SheetSpreadsheetId) = withContext(ioDispatcher) {
        database.getBySheetSpreadsheetId(
            sheetSpreadsheetId.spreadsheetId,
            sheetSpreadsheetId.sheetId
        ).executeAsOneOrNull()?.let(sheetMapper::mapToDomain)
    }

    override suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant) = withContext(ioDispatcher) {
        database.updateLastUpdatedAt(id = sheetId.value, last_updated_at = lastUpdatedAt.epochSecond)
    }
}