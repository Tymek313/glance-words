package com.example.words.widget

import com.example.words.logging.Logger
import com.example.words.logging.e
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.repository.WidgetLoadingStateNotifier
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class WordsWidgetViewModel(
    private val widgetId: Widget.WidgetId,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    wordsRepository: WordsRepository,
    private val widgetLoadingStateNotifier: WidgetLoadingStateNotifier,
    private val logger: Logger,
    private val locale: Locale,
    private val zoneId: ZoneId
) {
    private val shouldReshuffle = MutableStateFlow(false)
    private var isFirstWordsEmission = true

    val widgetDetailsState: Flow<WidgetDetailsState> = widgetSettingsRepository.observeSettings(widgetId)
        .filterNotNull()
        .map { widgetSettings ->
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
    val wordsState: Flow<WidgetWordsState> = wordsRepository.observeWords(widgetId)
        .flatMapLatest { wordPairs ->
            shouldReshuffle.map { wordPairs.shuffled().take(50) }
        }.flatMapLatest { wordPairs ->
            flow {
                // This flow starts if there are words emitted so we can just pass false as the first element
                emit(wordPairs to false)
                emitAll(widgetLoadingStateNotifier.observeIsWidgetLoading(widgetId)
                    .drop(if(isFirstWordsEmission) 1 else 0)
                    // We don't want to react to false here because it will be emitted immediately after reading true
                    // Besides we don't need to read false as the entire flow chain will be restarted when words come from `observeWords` or `observeWordsUpdates`
                    .map { wordPairs to true }
                )
            }
        }.onEach {
            if(isFirstWordsEmission) isFirstWordsEmission = false
        }.map { (words, isLoading) ->
            when {
                isLoading -> WidgetWordsState.Loading
                else -> WidgetWordsState.Success(words)
            }
        }.catch { error ->
            logger.e(javaClass.name, error)
            emit(WidgetWordsState.Failure)
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

sealed interface WidgetWordsState {
    data object Loading : WidgetWordsState
    data object Failure : WidgetWordsState
    data class Success(val words: List<WordPair>) : WidgetWordsState
}