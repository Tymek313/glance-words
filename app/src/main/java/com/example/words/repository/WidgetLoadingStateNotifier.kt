package com.example.words.repository

import com.example.words.model.Widget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

interface WidgetLoadingStateNotifier {
    fun observeIsWidgetLoading(widgetId: Widget.WidgetId): Flow<Unit>
    suspend fun setIsWidgetLoading(widgetId: Widget.WidgetId)
}

// TODO: Check for more performant solution
class DefaultWidgetLoadingStateNotifier : WidgetLoadingStateNotifier {
    private val widgetToLoadFlow = MutableStateFlow<Set<Widget.WidgetId>>(HashSet())

    override fun observeIsWidgetLoading(widgetId: Widget.WidgetId): Flow<Unit> {
        return widgetToLoadFlow
            .filter { it.contains(widgetId) }
            .onEach { widgetToLoadFlow.update { it - widgetId } }
            .map {  }
    }

    override suspend fun setIsWidgetLoading(widgetId: Widget.WidgetId) {
        widgetToLoadFlow.update { it + widgetId }
    }
}