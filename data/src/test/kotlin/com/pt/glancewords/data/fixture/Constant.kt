package com.pt.glancewords.data.fixture

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WidgetId
import java.time.Instant

val WORD_PAIR = randomWordPair()
val DB_SHEET = randomDbSheet()
val SHEET_SPREADSHEET_ID = DB_SHEET.run { SheetSpreadsheetId(spreadsheet_id, sheet_id) }
val WIDGET_ID = randomWidgetId()
val DB_WIDGET = randomDbWidget().copy(sheet_id = DB_SHEET.id)
val NEW_SHEET = DB_SHEET.run { NewSheet(SHEET_SPREADSHEET_ID, name) }
val EXISTING_SHEET = DB_SHEET.run {
    Sheet(
        id = SheetId(id),
        sheetSpreadsheetId = SheetSpreadsheetId(spreadsheet_id, sheet_id),
        name = name,
        lastUpdatedAt = Instant.ofEpochSecond(last_updated_at!!)
    )
}
val WIDGET_WITH_EXISTING_SHEET = DB_WIDGET.run {
    Widget(
        id = WidgetId(id),
        sheet = EXISTING_SHEET
    )
}