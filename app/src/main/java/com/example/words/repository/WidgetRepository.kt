package com.example.words.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.words.database.DbWidget
import com.example.words.database.DbWidgetQueries
import com.example.words.database.GetById
import com.example.words.model.Sheet
import com.example.words.model.SheetId
import com.example.words.model.SheetSpreadsheetId
import com.example.words.model.Widget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant

interface WidgetRepository {
    fun observeWidget(widgetId: Widget.WidgetId): Flow<Widget?>
    suspend fun addWidget(widget: Widget): Widget
    suspend fun deleteWidget(widgetId: Widget.WidgetId)
}

class DefaultWidgetRepository(
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
        val sheet = sheetRepository.addSheet(widget.sheet)
        database.insert(DbWidget(id = widget.id.value, sheet_id = sheet.id.value))
        widget.copy(sheet = sheet)
    }

    override suspend fun deleteWidget(widgetId: Widget.WidgetId) = withContext(ioDispatcher) {
        database.delete(widgetId.value)
    }
}