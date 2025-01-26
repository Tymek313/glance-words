package com.pt.glancewords.domain.synchronization

import com.pt.glancewords.domain.model.Widget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface WordsSynchronizationStateNotifier {
    fun observeAreWordsSynchronized(widgetId: Widget.WidgetId): Flow<Boolean>
    suspend fun notifyWordsSynchronizationForAction(widgetId: Widget.WidgetId, action: suspend () -> Unit)
}

class DefaultWordsSynchronizationStateNotifier : WordsSynchronizationStateNotifier {
    private val widgetToLoadFlow = MutableStateFlow<Set<Widget.WidgetId>>(HashSet())

    override fun observeAreWordsSynchronized(widgetId: Widget.WidgetId): Flow<Boolean> {
        return widgetToLoadFlow.map { it.contains(widgetId) }
    }

    override suspend fun notifyWordsSynchronizationForAction(widgetId: Widget.WidgetId, action: suspend () -> Unit) {
        widgetToLoadFlow.update { it + widgetId }
        action()
        widgetToLoadFlow.update { it - widgetId }
    }
}