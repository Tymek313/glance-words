package com.example.words.widget

import com.example.words.logging.Logger
import com.example.words.logging.e
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.repository.WidgetLoadingStateNotifier
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class WordsWidgetViewModel(
    private val widgetId: Widget.WidgetId,
    private val widgetRepository: WidgetRepository,
    private val wordsRepository: WordsRepository,
    widgetLoadingStateNotifier: WidgetLoadingStateNotifier,
    private val logger: Logger,
    private val locale: Locale,
    private val zoneId: ZoneId
) {
    private val shouldReshuffle = MutableStateFlow(false)

    val widgetDetailsState: Flow<WidgetDetailsState> = widgetRepository.observeWidget(widgetId)
        .filterNotNull()
        .map { widget ->
            WidgetDetailsState(
                sheetName = widget.sheet.name,
                lastUpdatedAt = widget.sheet.lastUpdatedAt?.let { lastUpdatedAt ->
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withLocale(locale)
                        .withZone(zoneId)
                        .format(lastUpdatedAt)
                }.orEmpty()
            )
        }

    val wordsState: Flow<WidgetWordsState> = combine(
        observeShuffledLimitedWords(),
        widgetLoadingStateNotifier.observeIsWidgetLoading(widgetId),
        ::mapWordsState
    ).catch { error ->
        logger.e(javaClass.name, error)
        emit(WidgetWordsState.Failure)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeShuffledLimitedWords() = wordsRepository.observeWords(widgetId)
        .flatMapLatest { wordPairs -> shouldReshuffle.map { wordPairs.shuffled().take(50) } }

    private fun mapWordsState(words: List<WordPair>, isLoading: Boolean): WidgetWordsState {
        return if (isLoading) {
            WidgetWordsState.Loading
        } else {
            WidgetWordsState.Success(words)
        }
    }

    fun reshuffleWords() {
        shouldReshuffle.value = !shouldReshuffle.value
    }

    suspend fun deleteWidget() {
        widgetRepository.deleteWidget(widgetId)
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