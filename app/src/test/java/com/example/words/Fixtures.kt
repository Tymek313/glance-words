package com.example.words

import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.repository.WordsRepository
import java.util.UUID
import kotlin.random.Random

fun getRandomWidgetId() = Widget.WidgetId(Random.nextInt())

fun getRandomSynchronizationRequest() = WordsRepository.SynchronizationRequest(
    widgetId = getRandomWidgetId(),
    spreadsheetId = UUID.randomUUID().toString(),
    sheetId = Random.nextInt()
)

fun getRandomWidget() = Widget(
    id = getRandomWidgetId(),
    spreadsheetId = UUID.randomUUID().toString(),
    sheetId = Random.nextInt(),
    sheetName = UUID.randomUUID().toString(),
    lastUpdatedAt = null
)

fun getRandomWordPair() = WordPair(UUID.randomUUID().toString(), UUID.randomUUID().toString())
