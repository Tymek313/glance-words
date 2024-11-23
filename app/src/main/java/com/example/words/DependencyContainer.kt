package com.example.words

import com.example.words.domain.WordsSynchronizationStateNotifier
import com.example.words.domain.WordsSynchronizer
import com.example.words.logging.Logger
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import com.example.words.widget.ReshuffleNotifier

interface DependencyContainer {
    val wordsRepository: WordsRepository
    val widgetRepository: WidgetRepository
    val wordsSynchronizer: WordsSynchronizer
    val spreadsheetRepository: SpreadsheetRepository
    val logger: Logger
    val wordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier
    val reshuffleNotifier: ReshuffleNotifier
}