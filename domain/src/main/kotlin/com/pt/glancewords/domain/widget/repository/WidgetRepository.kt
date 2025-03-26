package com.pt.glancewords.domain.widget.repository

import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.widget.model.Widget
import com.pt.glancewords.domain.widget.model.WidgetId
import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    suspend fun getWidget(widgetId: WidgetId): Widget?
    fun observeWidget(widgetId: WidgetId): Flow<Widget?>
    suspend fun addWidget(widgetId: WidgetId, sheetId: SheetId)
    suspend fun deleteWidget(widgetId: WidgetId)
}
