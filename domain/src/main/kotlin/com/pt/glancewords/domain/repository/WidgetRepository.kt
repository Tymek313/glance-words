package com.pt.glancewords.domain.repository

import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WidgetId
import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    fun observeWidget(widgetId: WidgetId): Flow<Widget?>
    suspend fun addWidget(widgetId: WidgetId, sheetId: SheetId)
    suspend fun deleteWidget(widgetId: WidgetId)
}