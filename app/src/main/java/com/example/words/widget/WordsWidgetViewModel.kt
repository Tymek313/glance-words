package com.example.words.widget

import com.example.words.domain.WidgetLoadingStateNotifier
import com.example.words.logging.Logger
import com.example.words.logging.d
import com.example.words.logging.e
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
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
    private val widgetRepository: WidgetRepository,
    private val wordsRepository: WordsRepository,
    widgetLoadingStateNotifier: WidgetLoadingStateNotifier,
    private val logger: Logger,
    private val locale: Locale,
    private val zoneId: ZoneId,
    private val reshuffleNotifier: ReshuffleNotifier
) {

    val uiState: Flow<WidgetUiState> = combine(
        observeWidget(),
        observeShuffledLimitedWords(),
        widgetLoadingStateNotifier.observeIsWidgetLoading(widgetId)
            .combine(reshuffleNotifier.shouldReshuffle) { loading, reshuffling -> loading || reshuffling },
        ::mapUiState
    ).catch { logger.e(this@WordsWidgetViewModel, throwable = it) }

    private fun observeWidget() = widgetRepository.observeWidget(widgetId)
        .onEach { if (it == null) logger.d(this, "Widget not found: ID = $widgetId") }
        .filterNotNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeShuffledLimitedWords() = flow {
        emit(true)
        emitAll(reshuffleNotifier.shouldReshuffle.filter { it })
    }.flatMapLatest {
        wordsRepository.observeWords(widgetId).map { it.shuffled().take(50) }
    }

    private fun mapUiState(widget: Widget, words: List<WordPair>, isLoading: Boolean) = WidgetUiState(
        sheetName = widget.sheet.name,
        lastUpdatedAt = widget.sheet.lastUpdatedAt?.let { lastUpdatedAt ->
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zoneId)
                .format(lastUpdatedAt)
        }.orEmpty(),
        words = words,
        isLoading = isLoading
    )

    suspend fun deleteWidget() {
        widgetRepository.deleteWidget(widgetId)
    }
}

data class WidgetUiState(
    val sheetName: String = "",
    val lastUpdatedAt: String = "",
    val isLoading: Boolean = false,
    val words: List<WordPair> = emptyList()
)