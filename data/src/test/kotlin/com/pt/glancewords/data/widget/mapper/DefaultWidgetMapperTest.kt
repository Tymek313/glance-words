package com.pt.glancewords.data.widget.mapper

import com.pt.glancewords.data.database.DbWidget
import com.pt.glancewords.data.database.GetById
import com.pt.glancewords.data.fixture.randomSheet
import com.pt.glancewords.data.fixture.randomWidgetId
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.widget.model.Widget
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.testcommon.fixture.randomInt
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DefaultWidgetMapperTest {

    private lateinit var mapper: DefaultWidgetMapper

    @Before
    fun setUp() {
        mapper = DefaultWidgetMapper()
    }

    @Test
    fun `when mapping to domain_then domain widget is returned`() {
        val result = mapper.mapToDomain(WIDGET_GET_BY_ID)

        assertEquals(WIDGET, result)
    }

    @Test
    fun `when mapping to domain_given sheet was never updated_then widget without sheet update timestamp is returned`() {
        val result = mapper.mapToDomain(WIDGET_GET_BY_ID.copy(s_last_updated_at = null))

        assertEquals(
            WIDGET.copy(sheet = WIDGET.sheet.copy(lastUpdatedAt = null)),
            result
        )
    }

    @Test
    fun `when mapping to database_given_then`() {
        val widgetIdValue = randomInt()
        val widgetId = WidgetId(widgetIdValue)
        val sheetIdValue = randomInt()
        val sheetId = SheetId(sheetIdValue)

        val result = mapper.mapToDb(widgetId, sheetId)

        assertEquals(DbWidget(widgetIdValue, sheetIdValue), result)
    }

    private companion object {
        val WIDGET = Widget(id = randomWidgetId(), sheet = randomSheet())
        val WIDGET_GET_BY_ID = GetById(
            id = WIDGET.id.value,
            sheet_id = WIDGET.sheet.id.value,
            s_spreadsheet_id = WIDGET.sheet.sheetSpreadsheetId.spreadsheetId,
            s_sheet_id = WIDGET.sheet.sheetSpreadsheetId.sheetId,
            s_name = WIDGET.sheet.name,
            s_last_updated_at = WIDGET.sheet.lastUpdatedAt!!.epochSecond
        )
    }
}