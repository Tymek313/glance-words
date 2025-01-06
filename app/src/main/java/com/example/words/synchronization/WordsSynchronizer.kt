package com.example.words.synchronization

import com.example.words.model.Widget
import com.example.words.repository.SheetRepository
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

interface WordsSynchronizer {
    suspend fun synchronizeWords(widgetId: Widget.WidgetId)
}

class DefaultWordsSynchronizer(
    private val wordsRepository: WordsRepository,
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier,
    private val refreshWidget: suspend (widgetId: Widget.WidgetId) -> Unit,
    private val getNowInstant: () -> Instant
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: Widget.WidgetId) {
        // Delete cached words to avoid loading them when widget restarts to prevent blinking
        wordsRepository.deleteCachedWords(widgetId)
        val widget = widgetRepository.observeWidget(widgetId).first().let(::checkNotNull)
        refreshWidget(widgetId)
        wordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction(widgetId) {
            wordsRepository.synchronizeWords(
                WordsRepository.SynchronizationRequest(widget.id, widget.sheet.sheetSpreadsheetId)
            )
            sheetRepository.updateLastUpdatedAt(widget.sheet.id, getNowInstant())
        }
    }
}
