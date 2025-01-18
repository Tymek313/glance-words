package com.example.words.data.repository

import com.example.domain.model.Widget
import com.example.domain.model.WordPair
import com.example.domain.repository.WordsRepository
import com.example.words.data.datasource.WordsLocalDataSource
import com.example.words.data.datasource.WordsRemoteDataSource
import com.example.words.data.mapper.WordPairMapper
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

    override fun observeWords(widgetId: Widget.WidgetId): Flow<List<WordPair>> = flow {
        localDataSource.getWords(widgetId)?.let { words ->
            emit(words.map(wordPairMapper::map))
        }
        emitAll(synchronizationUpdates.filter { it.widgetId == widgetId }.map { it.words })
    }

    override suspend fun synchronizeWords(request: WordsRepository.SynchronizationRequest) {
        val remoteCSV = remoteDataSource.getWords(request.sheetSpreadsheetId)
        localDataSource.storeWords(request.widgetId, remoteCSV)
        synchronizationUpdates.emit(
            SpreadsheetUpdate(request.widgetId, remoteCSV.map(wordPairMapper::map))
        )
    }

    override suspend fun deleteCachedWords(widgetId: Widget.WidgetId) {
        localDataSource.deleteWords(widgetId)
    }

    private class SpreadsheetUpdate(val widgetId: Widget.WidgetId, val words: List<WordPair>)
}