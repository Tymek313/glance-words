package com.example.words.repository

import com.example.words.datasource.WordsLocalDataSource
import com.example.words.datasource.WordsRemoteDataSource
import com.example.words.mapper.WordPairMapper
import com.example.words.model.SheetSpreadsheetId
import com.example.words.model.Widget
import com.example.words.model.WordPair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface WordsRepository {
    fun observeWords(widgetId: Widget.WidgetId): Flow<List<WordPair>>
    suspend fun synchronizeWords(request: SynchronizationRequest)
    suspend fun deleteCachedWords(widgetId: Widget.WidgetId)
    data class SynchronizationRequest(val widgetId: Widget.WidgetId, val sheetSpreadsheetId: SheetSpreadsheetId)
}

class DefaultWordsRepository(
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