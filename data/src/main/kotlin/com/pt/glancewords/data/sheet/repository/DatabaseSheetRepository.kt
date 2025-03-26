package com.pt.glancewords.data.sheet.repository

import com.pt.glancewords.data.database.DbSheetQueries
import com.pt.glancewords.data.sheet.mapper.SheetMapper
import com.pt.glancewords.domain.sheet.model.NewSheet
import com.pt.glancewords.domain.sheet.model.Sheet
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.sheet.model.SheetSpreadsheetId
import com.pt.glancewords.domain.sheet.repository.SheetRepository
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class DatabaseSheetRepository(
    private val database: DbSheetQueries,
    private val sheetMapper: SheetMapper,
    private val ioDispatcher: CoroutineDispatcher
) : SheetRepository {

    override suspend fun getSheets(): List<Sheet> = withContext(ioDispatcher) {
        database.getAll().executeAsList().map(sheetMapper::mapToDomain)
    }

    override suspend fun addSheet(sheet: NewSheet) = withContext(ioDispatcher) {
        val sheetId = database.transactionWithResult {
            database.insert(sheetMapper.mapToDb(sheet))
            database.getLastId().executeAsOne().toInt().let(::SheetId)
        }
        sheetMapper.mapToDomain(sheet, sheetId)
    }

    override suspend fun deleteSheet(sheetId: SheetId) {
        database.deleteById(sheetId.value)
    }

    override suspend fun getBySheetSpreadsheetId(sheetSpreadsheetId: SheetSpreadsheetId) = withContext(ioDispatcher) {
        database.getBySheetSpreadsheetId(
            sheetSpreadsheetId.spreadsheetId,
            sheetSpreadsheetId.sheetId
        ).executeAsOneOrNull()?.let(sheetMapper::mapToDomain)
    }

    override suspend fun exists(sheetId: SheetId) = withContext(ioDispatcher) {
        database.exists(sheetId.value).executeAsOne()
    }

    override suspend fun updateLastUpdatedAt(sheetId: SheetId, lastUpdatedAt: Instant) = withContext(ioDispatcher) {
        database.updateLastUpdatedAt(id = sheetId.value, last_updated_at = lastUpdatedAt.epochSecond)
    }
}
