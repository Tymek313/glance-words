package com.example.words.fixture

import com.example.words.database.DbSheet
import com.example.words.database.DbWidget
import com.example.words.model.Sheet
import com.example.words.model.SheetId
import com.example.words.model.SheetSpreadsheetId
import com.example.words.model.SpreadsheetSheet
import com.example.words.model.Widget
import com.example.words.model.WordPair
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

fun randomString() = UUID.randomUUID().toString()

fun randomInt() = Random.nextInt()

fun randomInstant(): Instant = Instant.ofEpochSecond(randomEpochSeconds())

fun randomEpochSeconds() = Random.nextLong(Instant.MIN.epochSecond, Instant.MAX.epochSecond)

fun randomWidgetId() = Widget.WidgetId(randomInt())

fun randomWidgetWithNewSheet() = Widget(
    id = randomWidgetId(),
    sheet = randomNewSheet()
)

fun randomDbWidget() = DbWidget(id = randomInt(), sheet_id = randomInt())

fun randomDbSheet() = DbSheet(
    id = randomInt(),
    spreadsheet_id = randomString(),
    sheet_id = randomInt(),
    name = randomString(),
    last_updated_at = randomEpochSeconds()
)

fun randomSheetSpreadsheetId() = SheetSpreadsheetId(
    spreadsheetId = randomString(),
    sheetId = randomInt()
)

fun randomNewSheet() = Sheet.createNew(
    sheetSpreadsheetId = randomSheetSpreadsheetId(),
    name = randomString(),
)

fun randomExistingSheet() = Sheet.createExisting(
    id = randomSheetId(),
    sheetSpreadsheetId = randomSheetSpreadsheetId(),
    name = randomString(),
    lastUpdatedAt = randomInstant()
)

fun randomSheetId() = SheetId(randomInt())

fun randomWordPair() = WordPair(randomString(), randomString())

fun randomSpreadsheetSheet() = SpreadsheetSheet(id = randomInt(), name = randomString())