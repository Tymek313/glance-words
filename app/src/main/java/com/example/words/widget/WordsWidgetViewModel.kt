package com.example.words.widget

import com.example.words.logging.Logger
import com.example.words.logging.e
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class WordsWidgetViewModel(
    private val widgetId: Widget.WidgetId,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val wordsSynchronizer: WordsSynchronizer,
    private val wordsRepository: WordsRepository,
    private val logger: Logger,
    private val locale: Locale,
    private val zoneId: ZoneId
) {
    private val shouldReload = MutableStateFlow(false)
    private val isLoadingFlow = MutableStateFlow(false)
    private val widgetSettings = widgetSettingsRepository.observeSettings(widgetId).filterNotNull()

    val widgetDetailsState: Flow<WidgetDetailsState> = widgetSettings.map { widgetSettings ->
        WidgetDetailsState(
            sheetName = widgetSettings.sheetName,
            lastUpdatedAt = widgetSettings.lastUpdatedAt?.let { lastUpdatedAt ->
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(locale)
                    .withZone(zoneId)
                    .format(lastUpdatedAt)
            }.orEmpty()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val wordsState: Flow<WidgetState> = widgetSettings
        .distinctUntilChanged { old, new -> old.spreadsheetId == new.spreadsheetId && old.sheetId == new.sheetId }
        .combine(shouldReload) { widget, _ -> widget }
        .flatMapLatest { widget ->
            wordsRepository.observeWords(widget.id)
                .map { it?.shuffled()?.take(50) }
                .map { words -> if (words == null) WidgetState.Failure else WidgetState.Success(words) }
                .onEach { isLoadingFlow.value = false }
        }
        .combine(isLoadingFlow) { widgetState, isLoading -> if (isLoading) WidgetState.InProgress else widgetState }
        .catch { error ->
            logger.e(javaClass.name, error)
            emit(WidgetState.Failure)
        }

    fun reshuffleWords() {
        shouldReload.value = !shouldReload.value
    }

    suspend fun synchronizeWords() {
        isLoadingFlow.value = true
        wordsSynchronizer.synchronizeWords(widgetId)
    }

    suspend fun deleteWidget() {
        widgetSettingsRepository.deleteWidget(widgetId)
    }
}

data class WidgetDetailsState(
    val sheetName: String,
    val lastUpdatedAt: String,
) {
    companion object {
        val EMPTY = WidgetDetailsState(sheetName = "", lastUpdatedAt = "")
    }
}

sealed interface WidgetState {
    data object InProgress : WidgetState
    data object Failure : WidgetState
    data class Success(val words: List<WordPair>) : WidgetState
}