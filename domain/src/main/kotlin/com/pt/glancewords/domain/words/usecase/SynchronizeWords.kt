package com.pt.glancewords.domain.words.usecase

import com.pt.glancewords.domain.sheet.repository.SheetRepository
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.domain.widget.repository.WidgetRepository
import com.pt.glancewords.domain.words.repository.WordsRepository
import com.pt.glancewords.domain.words.synchronization.WordsSynchronizationStateNotifier
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.e
import java.time.Instant

interface SynchronizeWords {
    suspend operator fun invoke(widgetId: WidgetId): Boolean
}

class DefaultSynchronizeWords(
    private val wordsRepository: WordsRepository,
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier,
    private val refreshWidget: suspend (widgetId: WidgetId) -> Unit,
    private val logger: Logger,
    private val getNowInstant: () -> Instant
) : SynchronizeWords {

    override suspend fun invoke(widgetId: WidgetId): Boolean {
        val widget = widgetRepository.getWidget(widgetId)

        return if (widget == null) {
            logger.e(this, "Widget is null")
            false
        } else {
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
