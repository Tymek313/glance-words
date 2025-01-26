package com.pt.glancewords.domain.repository

import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WordPair
import kotlinx.coroutines.flow.Flow

interface WordsRepository {
    fun observeWords(widgetId: Widget.WidgetId): Flow<List<WordPair>>
    suspend fun synchronizeWords(request: SynchronizationRequest)
    suspend fun deleteCachedWords(widgetId: Widget.WidgetId)
    data class SynchronizationRequest(val widgetId: Widget.WidgetId, val sheetSpreadsheetId: SheetSpreadsheetId)
}