package com.pt.glancewords.domain.repository

import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.model.WordPair
import kotlinx.coroutines.flow.Flow

interface WordsRepository {
    fun observeWords(widgetId: WidgetId): Flow<List<WordPair>>
    suspend fun synchronizeWords(request: SynchronizationRequest): Boolean
    suspend fun deleteCachedWords(widgetId: WidgetId)
    data class SynchronizationRequest(val widgetId: WidgetId, val sheetSpreadsheetId: SheetSpreadsheetId)
}