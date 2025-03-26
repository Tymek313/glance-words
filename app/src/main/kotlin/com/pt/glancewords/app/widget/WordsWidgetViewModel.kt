package com.pt.glancewords.app.widget

import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.widget.model.Widget
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.domain.widget.repository.WidgetRepository
import com.pt.glancewords.domain.words.model.WordPair
import com.pt.glancewords.domain.words.repository.WordsRepository
import com.pt.glancewords.domain.words.synchronization.WordsSynchronizationStateNotifier
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.d
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class WordsWidgetViewModel(
    private val widgetId: WidgetId,
    private val widgetRepository: WidgetRepository,
    private val wordsRepository: WordsRepository,
    wordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier,
    private val logger: Logger,
    private val reshuffleNotifier: ReshuffleNotifier
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: Flow<WidgetUiState> = combine(
        observeWidget().flatMapLatest { widget ->
            observeShuffledLimitedWords(widget.sheet.id).map { words -> widget to words }
        },
        wordsSynchronizationStateNotifier.observeAreWordsSynchronized(widgetId),
        transform = { (widget, words), isLoading -> mapUiState(widget, words, isLoading) }
    )

    private fun observeWidget() = widgetRepository.observeWidget(widgetId)
        .onEach { if (it == null) logger.d(this, "Widget not found: ID = $widgetId") }
        .filterNotNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeShuffledLimitedWords(sheetId: SheetId) = flow {
        emit(true)
        emitAll(reshuffleNotifier.reshuffleEvents.receiveAsFlow())
    }.flatMapLatest {
        wordsRepository.observeWords(sheetId).map { it.shuffled().take(50) }
    }

    private fun mapUiState(widget: Widget, words: List<WordPair>, isLoading: Boolean) = WidgetUiState(
        sheetName = widget.sheet.name,
        lastUpdatedAt = widget.sheet.lastUpdatedAt,
        words = words,
        isLoading = isLoading
    )

    suspend fun deleteWidget() {
        widgetRepository.deleteWidget(widgetId)
    }
}

data class WidgetUiState(
    val sheetName: String = "",
    val lastUpdatedAt: Instant? = null,
    val isLoading: Boolean = false,
    val words: List<WordPair> = emptyList()
)
