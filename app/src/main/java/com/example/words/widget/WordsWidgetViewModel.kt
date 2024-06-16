package com.example.words.widget

import com.example.words.logging.Logger
import com.example.words.logging.e
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.repository.WidgetLoadingStateSynchronizer
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class WordsWidgetViewModel(
    private val widgetId: Widget.WidgetId,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val wordsRepository: WordsRepository,
    private val widgetLoadingStateSynchronizer: WidgetLoadingStateSynchronizer,
    private val logger: Logger,
    private val locale: Locale,
    private val zoneId: ZoneId
) {
    private val shouldReshuffle = MutableStateFlow(false)
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
    val wordsState: Flow<WidgetState> = widgetLoadingStateSynchronizer.observeIsWidgetLoading(widgetId)
        // Take only first element when the viewmodel / widget is recreated to know
        // if we should read data from the local storage.
        // We don't want widget blinking so when words synchronization is ongoing we don't want to load local data
        .take(1)
        .flatMapLatest { isLoading ->
            if (isLoading) {
                wordsRepository.observeWordsUpdates(widgetId)
            } else {
                wordsRepository.observeWords(widgetId)
            }.flatMapLatest { wordPairs ->
                shouldReshuffle.map { wordPairs?.shuffled()?.take(50) }
            }.flatMapLatest { wordPairs ->
                flow {
                    // This flow starts if there are words emitted so we can just pass false as the first element
                    emit(wordPairs to false)
                    emitAll(widgetLoadingStateSynchronizer.observeIsWidgetLoading(widgetId)
                        // The first one should be false (above) so we can drop the first element emitted
                        .drop(1)
                        // We don't want to react to false here because it will be emitted immediately after reading true
                        // Besides we don't need to read false the entire flow chain will be restarted when words come from `observeWords` or `observeWordsUpdates`
                        .filter { it }
                        .map { isLoading -> wordPairs to isLoading })
                }
            }.map { (words, isLoading) ->
                when {
                    isLoading -> WidgetState.InProgress
                    words == null -> WidgetState.Failure
                    else -> WidgetState.Success(words)
                }
            }
        }.catch { error ->
            logger.e(javaClass.name, error)
            emit(WidgetState.Failure)
        }

    fun reshuffleWords() {
        shouldReshuffle.value = !shouldReshuffle.value
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