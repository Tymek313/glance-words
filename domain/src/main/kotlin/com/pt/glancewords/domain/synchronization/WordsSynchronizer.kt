package com.pt.glancewords.domain.synchronization

import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

interface WordsSynchronizer {
    suspend fun synchronizeWords(widgetId: WidgetId)
}

class DefaultWordsSynchronizer(
    private val wordsRepository: WordsRepository,
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier,
    private val refreshWidget: suspend (widgetId: WidgetId) -> Unit,
    private val getNowInstant: () -> Instant
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: WidgetId) {
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
