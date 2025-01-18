package com.example.words.data.fixture

import com.example.domain.model.Sheet
import com.example.domain.model.SheetId
import com.example.domain.model.SheetSpreadsheetId
import com.example.domain.model.Widget
import com.example.domain.model.WordPair
import com.example.testcommon.fixture.randomEpochSeconds
import com.example.testcommon.fixture.randomInstant
import com.example.testcommon.fixture.randomInt
import com.example.testcommon.fixture.randomString
import com.example.words.database.DbSheet
import com.example.words.database.DbWidget

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