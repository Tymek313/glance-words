package com.pt.glancewords.domain.usecase

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.w
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

interface AddWidget {
    suspend operator fun invoke(widgetId: WidgetId, newSheet: NewSheet): Boolean
}

class DefaultAddWidget(
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsRepository: WordsRepository,
    private val logger: Logger
) : AddWidget {

    override suspend fun invoke(widgetId: WidgetId, newSheet: NewSheet): Boolean {
        val existingSheet = sheetRepository.getBySheetSpreadsheetId(newSheet.sheetSpreadsheetId)

        val addWidgetSucceeded = if (existingSheet == null) {
            var storedSheet: Sheet? = null
            try {
                storedSheet = sheetRepository.addSheet(newSheet)
                val wordSyncSucceeded = wordsRepository.synchronizeWords(storedSheet.id, newSheet.sheetSpreadsheetId)
                if (wordSyncSucceeded) {
                    widgetRepository.addWidget(widgetId, storedSheet.id)
                } else {
                    sheetRepository.deleteSheet(storedSheet.id)
                }
                wordSyncSucceeded
            } catch (e: Exception) {
                // Due to coroutine cancellation issues with SQLDelight transaction implementation, rollback changes manually
                rollbackIfNeeded(storedSheet)
                if (e is CancellationException) {
                    throw e
                } else {
                    logger.w(this@DefaultAddWidget, e)
                    false
                }
            }
        } else {
            widgetRepository.addWidget(widgetId, existingSheet.id)
            true
        }

        return addWidgetSucceeded
    }

    private suspend fun rollbackIfNeeded(sheet: Sheet?) {
        if (sheet != null) {
            withContext(NonCancellable) {
                sheetRepository.deleteSheet(sheet.id)
            }
        }
    }
}
