package com.example.words.data.fixture

import com.example.domain.model.Sheet
import com.example.domain.model.SheetId
import com.example.domain.model.SheetSpreadsheetId
import com.example.domain.model.Widget
import java.time.Instant

val WORD_PAIR = randomWordPair()
val DB_SHEET = randomDbSheet()
val SHEET_SPREADSHEET_ID = DB_SHEET.run { SheetSpreadsheetId(spreadsheet_id, sheet_id) }
val WIDGET_ID = randomWidgetId()
val DB_WIDGET = randomDbWidget().copy(sheet_id = DB_SHEET.id)
val NEW_SHEET = DB_SHEET.run { Sheet.createNew(SHEET_SPREADSHEET_ID, name) }
val EXISTING_SHEET = DB_SHEET.run {
    Sheet.createExisting(
        id = SheetId(id),
        sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
        name = name,
        lastUpdatedAt = Instant.ofEpochSecond(last_updated_at!!)
    )
}
val WIDGET_WITH_EXISTING_SHEET = DB_WIDGET.run {
    Widget(
        id = Widget.WidgetId(id),
        sheet = EXISTING_SHEET
    )
}