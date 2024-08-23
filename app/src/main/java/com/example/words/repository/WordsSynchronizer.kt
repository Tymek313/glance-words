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
    private val refreshWidget: suspend (widgetId: Widget.WidgetId) -> Unit,
    private val getNowInstant: () -> Instant
) : WordsSynchronizer {

    override suspend fun synchronizeWords(widgetId: Widget.WidgetId) {
        val widgetSettings = widgetSettingsRepository.observeSettings(widgetId).first().let(::checkNotNull)
        refreshWidget(widgetId)
        widgetLoadingStateNotifier.setLoadingWidgetForAction(widgetId) {
            val syncRequest = widgetSettings.run { WordsRepository.SynchronizationRequest(id, spreadsheetId, sheetId) }
            wordsRepository.synchronizeWords(syncRequest)
            widgetSettingsRepository.updateLastUpdatedAt(widgetId, getNowInstant())
        }
    }
}