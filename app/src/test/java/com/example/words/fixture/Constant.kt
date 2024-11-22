package com.example.words.fixture

import com.example.words.model.Sheet
import com.example.words.model.SheetId
import com.example.words.model.SheetSpreadsheetId
import com.example.words.model.SpreadsheetSheet
import com.example.words.model.Widget
import java.time.Instant

val instantFixture = randomInstant()
val sheetSpreadsheetIdFixture = randomSheetSpreadsheetId()
val wordPairFixture = randomWordPair()
val dbSheetFixture = randomDbSheet()
val widgetIdFixture = randomWidgetId()
val dbWidgetFixture = randomDbWidget().copy(sheet_id = dbSheetFixture.id)
val newSheetFixture = randomNewSheet().copy(sheetSpreadsheetId = sheetSpreadsheetIdFixture)
val spreadsheetSheetForNewSheetFixture = SpreadsheetSheet(id = sheetSpreadsheetIdFixture.sheetId, name = newSheetFixture.name)
val existingSheetFixture = dbSheetFixture.run {
    Sheet.createExisting(
        id = SheetId(id),
        sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
        name = name,
        lastUpdatedAt = Instant.ofEpochSecond(last_updated_at!!)
    )
}
val widgetWithExistingSheetFixture = dbWidgetFixture.run {
    Widget(
        id = Widget.WidgetId(id),
        sheet = existingSheetFixture
    )
}
val widgetWithNewSheetFixture = dbWidgetFixture.run {
    Widget(
        id = Widget.WidgetId(id),
        sheet = newSheetFixture
    )
}