package com.example.words

import com.example.words.model.Widget
import com.example.words.model.WordPair
import java.util.UUID
import kotlin.random.Random

fun randomString() = UUID.randomUUID().toString()

fun randomInt() = Random.nextInt()

fun randomWidgetId() = Widget.WidgetId(randomInt())

fun randomWidget() = Widget(
    id = randomWidgetId(),
    spreadsheetId = randomString(),
    sheetId = randomInt(),
    sheetName = randomString(),
    lastUpdatedAt = null
)

fun randomWordPair() = WordPair(randomString(), randomString())