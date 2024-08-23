package com.example.words.repository

import com.example.words.model.Widget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface WidgetLoadingStateNotifier {
    fun observeIsWidgetLoading(widgetId: Widget.WidgetId): Flow<Boolean>
    suspend fun setLoadingWidgetForAction(widgetId: Widget.WidgetId, action: suspend () -> Unit)
}

class DefaultWidgetLoadingStateNotifier : WidgetLoadingStateNotifier {
    private val widgetToLoadFlow = MutableStateFlow<Set<Widget.WidgetId>>(HashSet())

    override fun observeIsWidgetLoading(widgetId: Widget.WidgetId): Flow<Boolean> {
        return widgetToLoadFlow.map { it.contains(widgetId) }
    }

    override suspend fun setLoadingWidgetForAction(widgetId: Widget.WidgetId, action: suspend () -> Unit) {
        widgetToLoadFlow.update { it + widgetId }
        action()
        widgetToLoadFlow.update { it - widgetId }
    }
}