package com.pt.glancewords.data.fixture

import com.pt.glancewords.data.database.DbSheet
import com.pt.glancewords.data.database.DbWidget
import com.pt.glancewords.domain.sheet.model.NewSheet
import com.pt.glancewords.domain.sheet.model.Sheet
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.sheet.model.SheetSpreadsheetId
import com.pt.glancewords.domain.widget.model.Widget
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.domain.words.model.WordPair
import com.pt.glancewords.testcommon.fixture.randomEpochSeconds
import com.pt.glancewords.testcommon.fixture.randomInstant
import com.pt.glancewords.testcommon.fixture.randomInt
import com.pt.glancewords.testcommon.fixture.randomString

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