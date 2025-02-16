package com.pt.glancewords.data.repository

import com.pt.glancewords.data.datasource.WordsLocalDataSource
import com.pt.glancewords.data.datasource.WordsRemoteDataSource
import com.pt.glancewords.data.mapper.WordPairMapper
import com.pt.glancewords.domain.model.WidgetId
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

    override fun observeWords(widgetId: WidgetId): Flow<List<WordPair>> = flow {
        localDataSource.getWords(widgetId)?.let { words ->
            emit(words.map(wordPairMapper::map))
        }
        emitAll(synchronizationUpdates.filter { it.widgetId == widgetId }.map { it.words })
    }

    override suspend fun synchronizeWords(request: WordsRepository.SynchronizationRequest): Boolean {
        val remoteCSV = remoteDataSource.getWords(request.sheetSpreadsheetId)
        return if (remoteCSV == null) {
            false
        } else {
            localDataSource.storeWords(request.widgetId, remoteCSV)
            synchronizationUpdates.emit(
                SpreadsheetUpdate(request.widgetId, remoteCSV.map(wordPairMapper::map))
            )
            true
        }
    }

    override suspend fun deleteCachedWords(widgetId: WidgetId) {
        localDataSource.deleteWords(widgetId)
    }

    private class SpreadsheetUpdate(val widgetId: WidgetId, val words: List<WordPair>)
}