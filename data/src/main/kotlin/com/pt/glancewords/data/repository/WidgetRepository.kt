package com.pt.glancewords.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.pt.glancewords.database.DbWidget
import com.pt.glancewords.database.DbWidgetQueries
import com.pt.glancewords.database.GetById
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

internal class DefaultWidgetRepository(
    private val database: DbWidgetQueries,
    private val sheetRepository: SheetRepository,
    private val ioDispatcher: CoroutineDispatcher
) : WidgetRepository {

    override fun observeWidget(widgetId: Widget.WidgetId): Flow<Widget?> {
        return database.getById(widgetId.value)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    private fun GetById.toDomain() = Widget(
        id = Widget.WidgetId(id),
        sheet = Sheet.createExisting(
            id = SheetId(sheet_id),
            sheetSpreadsheetId = SheetSpreadsheetId(s_spreadsheet_id, s_sheet_id),
            name = s_name,
            lastUpdatedAt = s_last_updated_at?.let(Instant::ofEpochSecond)
        )
    )

    override suspend fun addWidget(widget: Widget) = withContext(ioDispatcher) {
        val sheet = sheetRepository.getBySheetSpreadsheetId(widget.sheet.sheetSpreadsheetId) ?: sheetRepository.addSheet(widget.sheet)
        database.insert(DbWidget(id = widget.id.value, sheet_id = sheet.id.value))
        widget.copy(sheet = sheet)
    }

    override suspend fun deleteWidget(widgetId: Widget.WidgetId) = withContext(ioDispatcher) {
        database.delete(widgetId.value)
    }
}