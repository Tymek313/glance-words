package com.example.words.data.fixture

import com.example.domain.model.Sheet
import com.example.domain.model.SheetId
import com.example.domain.model.SheetSpreadsheetId
import com.example.domain.model.Widget
import java.time.Instant

val wordPairFixture = randomWordPair()
val dbSheetFixture = randomDbSheet()
val sheetSpreadsheetIdFixture = dbSheetFixture.run { SheetSpreadsheetId(spreadsheet_id, sheet_id) }
val widgetIdFixture = randomWidgetId()
val dbWidgetFixture = randomDbWidget().copy(sheet_id = dbSheetFixture.id)
val newSheetFixture = dbSheetFixture.run { Sheet.createNew(sheetSpreadsheetIdFixture, name) }
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