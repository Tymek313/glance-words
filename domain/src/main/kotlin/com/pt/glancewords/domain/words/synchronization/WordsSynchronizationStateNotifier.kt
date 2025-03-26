package com.pt.glancewords.domain.words.synchronization

import com.pt.glancewords.domain.widget.model.WidgetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface WordsSynchronizationStateNotifier {
    fun observeAreWordsSynchronized(widgetId: WidgetId): Flow<Boolean>
    suspend fun <T> notifyWordsSynchronizationForAction(widgetId: WidgetId, action: suspend () -> T): T
}

class DefaultWordsSynchronizationStateNotifier : WordsSynchronizationStateNotifier {
    private val widgetToLoadFlow = MutableStateFlow<Set<WidgetId>>(HashSet())

    override fun observeAreWordsSynchronized(widgetId: WidgetId): Flow<Boolean> = widgetToLoadFlow.map { it.contains(widgetId) }

    override suspend fun <T> notifyWordsSynchronizationForAction(widgetId: WidgetId, action: suspend () -> T): T {
        widgetToLoadFlow.update { it + widgetId }
        val result = action()
        widgetToLoadFlow.update { it - widgetId }
        return result
    }
}
