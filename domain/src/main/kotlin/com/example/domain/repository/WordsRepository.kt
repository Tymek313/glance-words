package com.example.domain.repository

import com.example.domain.model.SheetSpreadsheetId
import com.example.domain.model.Widget
import com.example.domain.model.WordPair
import kotlinx.coroutines.flow.Flow

interface WordsRepository {
    fun observeWords(widgetId: Widget.WidgetId): Flow<List<WordPair>>
    suspend fun synchronizeWords(request: SynchronizationRequest)
    suspend fun deleteCachedWords(widgetId: Widget.WidgetId)
    data class SynchronizationRequest(val widgetId: Widget.WidgetId, val sheetSpreadsheetId: SheetSpreadsheetId)
}