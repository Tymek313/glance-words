package com.pt.glancewords.domain.synchronization

import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.e
import java.time.Instant

interface WordsSynchronizer {
    suspend fun synchronizeWords(widgetId: WidgetId): Boolean
}

class DefaultWordsSynchronizer(
    private val wordsRepository: WordsRepository,
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier,
    private val refreshWidget: suspend (widgetId: WidgetId) -> Unit,
    private val logger: Logger,
    private val getNowInstant: () -> Instant
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: WidgetId): Boolean {
        val widget = widgetRepository.getWidget(widgetId)

        return if (widget == null) {
            logger.e(this, "Widget is null")
            false
        } else {
            // Delete cached words to avoid loading them when widget restarts to prevent blinking
            wordsRepository.deleteWords(widget.sheet.id)
            refreshWidget(widget.id)
            wordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction(widget.id) {
                val syncSucceeded = wordsRepository.synchronizeWords(widget.sheet.id, widget.sheet.sheetSpreadsheetId)
                if (syncSucceeded) {
                    sheetRepository.updateLastUpdatedAt(widget.sheet.id, getNowInstant())
                }
                syncSucceeded
            }
        }
    }
}
