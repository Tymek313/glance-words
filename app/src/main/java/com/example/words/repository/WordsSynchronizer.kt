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
    private val widgetLoadingStateNotifier: WidgetLoadingStateNotifier,
    private val updateWidget: suspend (widgetId: Widget.WidgetId) -> Unit,
    private val getNowInstant: () -> Instant
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: Widget.WidgetId) {
        val widgetSettings = widgetSettingsRepository.observeSettings(widgetId).first().let(::checkNotNull)
        // Delete cached words to avoid loading them when widget restarts to prevent blinking
        wordsRepository.deleteCachedWords(widgetId)
        updateWidget(widgetId)
        widgetLoadingStateNotifier.setIsWidgetLoading(widgetId)
        val syncRequest = widgetSettings.run { WordsRepository.SynchronizationRequest(id, spreadsheetId, sheetId) }
        wordsRepository.synchronizeWords(syncRequest)
        widgetSettingsRepository.updateLastUpdatedAt(widgetId, getNowInstant())
    }
}