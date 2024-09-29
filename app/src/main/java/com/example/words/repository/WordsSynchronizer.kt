package com.example.words.repository

import com.example.words.model.Widget
import kotlinx.coroutines.flow.first
import java.time.Instant

interface WordsSynchronizer {
    suspend fun synchronizeWords(widgetId: Widget.WidgetId)
}

class DefaultWordsSynchronizer(
    private val wordsRepository: WordsRepository,
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val widgetLoadingStateNotifier: WidgetLoadingStateNotifier,
    private val refreshWidget: suspend (widgetId: Widget.WidgetId) -> Unit,
    private val getNowInstant: () -> Instant
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: Widget.WidgetId) {
        val widget = widgetRepository.observeWidget(widgetId).first().let(::checkNotNull)
        refreshWidget(widgetId)
        widgetLoadingStateNotifier.setLoadingWidgetForAction(widgetId) {
            wordsRepository.synchronizeWords(
                WordsRepository.SynchronizationRequest(widget.id, widget.sheet.sheetSpreadsheetId)
            )
            sheetRepository.updateLastUpdatedAt(widget.sheet.id, getNowInstant())
        }
    }
}
