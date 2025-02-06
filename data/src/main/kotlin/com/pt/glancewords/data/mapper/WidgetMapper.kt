package com.pt.glancewords.data.mapper

import com.pt.glancewords.database.DbWidget
import com.pt.glancewords.database.GetById
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WidgetId
import java.time.Instant

interface WidgetMapper {
    fun mapToDomain(dbWidget: GetById): Widget
    fun mapToDb(widgetId: WidgetId, sheetId: SheetId): DbWidget
}

class DefaultWidgetMapper : WidgetMapper {
    override fun mapToDomain(dbWidget: GetById): Widget = with(dbWidget) {
        return Widget(
            id = WidgetId(id),
            sheet = Sheet(
                id = SheetId(sheet_id),
                sheetSpreadsheetId = SheetSpreadsheetId(s_spreadsheet_id, s_sheet_id),
                name = s_name,
                lastUpdatedAt = s_last_updated_at?.let(Instant::ofEpochSecond)
            )
        )
    }

    override fun mapToDb(widgetId: WidgetId, sheetId: SheetId) = DbWidget(id = widgetId.value, sheet_id = sheetId.value)
}