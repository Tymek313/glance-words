package com.pt.glancewords.domain.usecase

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository

interface AddWidget {
    suspend operator fun invoke(widgetId: WidgetId, newSheet: NewSheet): Boolean
}

class DefaultAddWidget(
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsRepository: WordsRepository
) : AddWidget {

    override suspend fun invoke(widgetId: WidgetId, newSheet: NewSheet): Boolean {
        val existingSheet = sheetRepository.getBySheetSpreadsheetId(newSheet.sheetSpreadsheetId)

        return if (existingSheet == null) {
            // Store within a transaction to avoid storing garbage data when activity is closed during synchronization
            val sheet = sheetRepository.addSheetInTransaction(newSheet) { sheetId ->
                wordsRepository.synchronizeWords(sheetId, newSheet.sheetSpreadsheetId)
            }
            if (sheet == null) {
                false
            } else {
                widgetRepository.addWidget(widgetId, sheet.id)
                true
            }
        } else {
            widgetRepository.addWidget(widgetId, existingSheet.id)
            true
        }
    }
}
