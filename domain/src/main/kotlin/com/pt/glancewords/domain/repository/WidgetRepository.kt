package com.pt.glancewords.domain.repository

import com.pt.glancewords.domain.model.Widget
import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    fun observeWidget(widgetId: Widget.WidgetId): Flow<Widget?>
    suspend fun addWidget(widget: Widget): Widget
    suspend fun deleteWidget(widgetId: Widget.WidgetId)
}