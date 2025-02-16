package com.pt.glancewords.domain.usecase

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.synchronization.WordsSynchronizer
import com.pt.glancewords.domain.usecase.AddWidget.WidgetToAdd

interface AddWidget {
    suspend operator fun invoke(widgetToAdd: WidgetToAdd): Boolean

    data class WidgetToAdd(val widgetId: WidgetId, val sheet: NewSheet)
}

class DefaultAddWidget(
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsSynchronizer: WordsSynchronizer
) : AddWidget {

    override suspend fun invoke(widgetToAdd: WidgetToAdd): Boolean {
        val sheet = sheetRepository.getBySheetSpreadsheetId(widgetToAdd.sheet.sheetSpreadsheetId) ?: sheetRepository.addSheet(widgetToAdd.sheet)
        widgetRepository.addWidget(widgetToAdd.widgetId, sheet.id)
        val syncSucceeded = wordsSynchronizer.synchronizeWords(widgetToAdd.widgetId)
        if (!syncSucceeded) {
            widgetRepository.deleteWidget(widgetToAdd.widgetId)
            // No need to delete sheet. It's handled by the trigger in the database
        }
        return syncSucceeded
    }
}
