package com.example.words.repository

import com.example.words.model.Widget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

interface WidgetLoadingStateSynchronizer {
    fun observeIsWidgetLoading(widgetId: Widget.WidgetId): Flow<Boolean>
    suspend fun setIsWidgetLoading(widgetId: Widget.WidgetId)
}

class DefaultWidgetLoadingStateSynchronizer: WidgetLoadingStateSynchronizer {
    private val widgetToLoadFlow = MutableStateFlow<Widget.WidgetId?>(null)

    override fun observeIsWidgetLoading(widgetId: Widget.WidgetId): Flow<Boolean> {
        return widgetToLoadFlow.onEach { widgetToLoadFlow.emit(null) }.map { it != null }
    }

    override suspend fun setIsWidgetLoading(widgetId: Widget.WidgetId) {
        widgetToLoadFlow.emit(widgetId)
    }
}