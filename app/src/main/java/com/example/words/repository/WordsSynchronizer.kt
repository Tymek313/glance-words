package com.example.words.repository

import kotlinx.coroutines.flow.first
import java.time.Instant

interface WordsSynchronizer {
    suspend fun synchronizeWords(widgetId: Int)
}

class DefaultWordsSynchronizer(
    private val wordsRepository: WordsRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: Int) {
        val widgetSettings = widgetSettingsRepository.observeSettings(widgetId).first().let(::checkNotNull)
        wordsRepository.synchronizeWords(widgetSettings.spreadsheetId, widgetSettings.sheetId)
        widgetSettingsRepository.updateLastUpdatedAt(widgetSettings.widgetId, Instant.now())
    }
}