package com.pt.glancewords.data.repository

import com.pt.glancewords.data.mapper.SheetMapper
import com.pt.glancewords.database.DbSheetQueries
import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.repository.SheetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Instant

internal class DefaultSheetRepository(
    private val database: DbSheetQueries,
    private val sheetMapper: SheetMapper,
    private val ioDispatcher: CoroutineDispatcher
) : SheetRepository {

    override suspend fun getSheets(): List<Sheet> = withContext(ioDispatcher) {
        database.getAll().executeAsList().map(sheetMapper::mapToDomain)
    }

    override suspend fun addSheetInTransaction(sheet: NewSheet, actionInTransaction: suspend (determinedId: SheetId) -> Boolean) = withContext(ioDispatcher) {
        val sheetId = database.transactionWithResult {
            database.insert(sheetMapper.mapToDb(sheet))
            val sheetId = database.getLastId().executeAsOne().toInt().let(::SheetId)
            val actionSucceeded = runBlocking(coroutineContext) { // Pass coroutine context to support coroutine cancellation
                actionInTransaction(sheetId)
            }
            if (!actionSucceeded) {
                rollback(null)
            }
            sheetId
        }
        sheetId?.let { sheetMapper.mapToDomain(sheet, it) }
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