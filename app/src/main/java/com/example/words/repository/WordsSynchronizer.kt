package com.example.words.repository

import com.example.words.model.Widget
import kotlinx.coroutines.flow.first
import java.time.Instant

interface WordsSynchronizer {
    suspend fun synchronizeWords(widgetId: Widget.WidgetId)
}

class DefaultWordsSynchronizer(
    private val wordsRepository: WordsRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val getNowInstant: () -> Instant
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: Widget.WidgetId) {
        val widgetSettings = widgetSettingsRepository.observeSettings(widgetId).first().let(::checkNotNull)
        wordsRepository.synchronizeWords(
            WordsRepository.SynchronizationRequest(
                widgetId = widgetSettings.id,
                spreadsheetId = widgetSettings.spreadsheetId,
                sheetId = widgetSettings.sheetId
            )
        )
        widgetSettingsRepository.updateLastUpdatedAt(widgetSettings.id, getNowInstant())
    }
}