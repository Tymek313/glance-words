package com.pt.glancewords.data.repository

import com.pt.glancewords.data.datasource.WordsLocalDataSource
import com.pt.glancewords.data.datasource.WordsRemoteDataSource
import com.pt.glancewords.data.mapper.WordPairMapper
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WordPair
import com.pt.glancewords.domain.repository.WordsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class DefaultWordsRepository(
    private val remoteDataSource: WordsRemoteDataSource,
    private val localDataSource: WordsLocalDataSource,
    private val wordPairMapper: WordPairMapper
) : WordsRepository {

    private val synchronizationUpdates = MutableSharedFlow<SpreadsheetUpdate>()

    override fun observeWords(sheetId: SheetId): Flow<List<WordPair>> = flow {
        localDataSource.getWords(sheetId)?.let { words ->
            emit(words.map(wordPairMapper::map))
        }
        emitAll(synchronizationUpdates.filter { it.sheetId == sheetId }.map { it.words })
    }

    override suspend fun synchronizeWords(sheetId: SheetId, sheetSpreadsheetId: SheetSpreadsheetId): Boolean {
        val remoteCSV = remoteDataSource.getWords(sheetSpreadsheetId)
        return if (remoteCSV == null) {
            false
        } else {
            localDataSource.storeWords(sheetId, remoteCSV)
            synchronizationUpdates.emit(
                SpreadsheetUpdate(sheetId, remoteCSV.map(wordPairMapper::map))
            )
            true
        }
    }

    override suspend fun deleteWords(sheetId: SheetId) {
        localDataSource.deleteWords(sheetId)
    }

    private class SpreadsheetUpdate(val sheetId: SheetId, val words: List<WordPair>)
}
