package com.pt.glancewords.domain.usecase

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.glancewords.domain.usecase.AddWidget.WidgetToAdd

interface AddWidget {
    suspend operator fun invoke(widgetToAdd: WidgetToAdd): Boolean

    data class WidgetToAdd(val widgetId: WidgetId, val sheet: NewSheet)
}

class DefaultAddWidget(
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsRepository: WordsRepository,
) : AddWidget {

    override suspend fun invoke(widgetToAdd: WidgetToAdd): Boolean {
        // Sync words before storing widget and sheet id database since it can fail
        val wordsSyncSucceeded = wordsRepository.synchronizeWords(
            WordsRepository.SynchronizationRequest(widgetToAdd.widgetId, widgetToAdd.sheet.sheetSpreadsheetId)
        )
        if (wordsSyncSucceeded) {
            val sheet = sheetRepository.getBySheetSpreadsheetId(widgetToAdd.sheet.sheetSpreadsheetId) ?: sheetRepository.addSheet(widgetToAdd.sheet)
            widgetRepository.addWidget(widgetToAdd.widgetId, sheet.id)
        }
        return wordsSyncSucceeded
    }
}
