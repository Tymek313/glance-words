package com.pt.glancewords.data.fixture

import com.pt.glancewords.database.DbSheet
import com.pt.glancewords.database.DbWidget
import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.model.WordPair
import com.pt.testcommon.fixture.randomEpochSeconds
import com.pt.testcommon.fixture.randomInstant
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString

fun randomWidgetId() = WidgetId(randomInt())

fun randomWidgetWithNewSheet() = Widget(
    id = randomWidgetId(),
    sheet = randomSheet()
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

fun randomNewSheet() = NewSheet(
    sheetSpreadsheetId = randomSheetSpreadsheetId(),
    name = randomString(),
)

fun randomSheet() = Sheet(
    id = randomSheetId(),
    sheetSpreadsheetId = randomSheetSpreadsheetId(),
    name = randomString(),
    lastUpdatedAt = randomInstant()
)

fun randomSheetId() = SheetId(randomInt())

fun randomWordPair() = WordPair(randomString(), randomString())