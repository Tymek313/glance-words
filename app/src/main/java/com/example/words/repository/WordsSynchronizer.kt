package com.example.words.repository

import kotlinx.coroutines.flow.first
import java.time.Instant

class WordsSynchronizer(
    private val wordsRepository: WordsRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository
) {

    suspend fun synchronizeWords(widgetId: Int) {
        val widgetSettings = widgetSettingsRepository.observeSettings(widgetId).first().let(::checkNotNull)
        wordsRepository.synchronizeWords(widgetSettings.spreadsheetId, widgetSettings.sheetId)
        widgetSettingsRepository.updateLastUpdatedAt(widgetSettings.widgetId, Instant.now())
    }
}