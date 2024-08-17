package com.example.words

import com.example.words.logging.Logger
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetLoadingStateNotifier
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer

interface DependencyContainer {
    val wordsRepository: WordsRepository
    val widgetSettingsRepository: WidgetSettingsRepository
    val wordsSynchronizer: WordsSynchronizer
    val spreadsheetRepository: SpreadsheetRepository
    val logger: Logger
    val widgetLoadingStateNotifier: WidgetLoadingStateNotifier
}