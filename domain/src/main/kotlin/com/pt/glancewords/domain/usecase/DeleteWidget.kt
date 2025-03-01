package com.pt.glancewords.domain.usecase

import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.e

interface DeleteWidget {
    suspend operator fun invoke(widgetId: WidgetId)
}

class DefaultDeleteWidget(
    private val widgetRepository: WidgetRepository,
    private val sheetRepository: SheetRepository,
    private val wordsRepository: WordsRepository,
    private val logger: Logger
) : DeleteWidget {

    override suspend fun invoke(widgetId: WidgetId) {
        val widget = widgetRepository.getWidget(widgetId) ?: return run {
            logger.e(this, "Widget not found. ID: $widgetId")
        }
        widgetRepository.deleteWidget(widgetId)
        val sheetExists = sheetRepository.exists(widget.sheet.id)
        if (!sheetExists) {
            wordsRepository.deleteWords(widget.sheet.id)
        }
    }
}