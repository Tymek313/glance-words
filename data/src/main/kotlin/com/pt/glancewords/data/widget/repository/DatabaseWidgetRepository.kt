package com.pt.glancewords.data.widget.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.pt.glancewords.data.database.DbWidgetQueries
import com.pt.glancewords.data.widget.mapper.WidgetMapper
import com.pt.glancewords.domain.sheet.model.SheetId
import com.pt.glancewords.domain.widget.model.Widget
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.domain.widget.repository.WidgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DatabaseWidgetRepository(
    private val database: DbWidgetQueries,
    private val widgetMapper: WidgetMapper,
    private val ioDispatcher: CoroutineDispatcher
) : WidgetRepository {

    override suspend fun getWidget(widgetId: WidgetId): Widget? = withContext(ioDispatcher) {
        database.getById(widgetId.value).executeAsOneOrNull()?.let(widgetMapper::mapToDomain)
    }

    override fun observeWidget(widgetId: WidgetId): Flow<Widget?> = database.getById(widgetId.value)
        .asFlow()
        .mapToOneOrNull(ioDispatcher)
        .map { dbWidget -> dbWidget?.let(widgetMapper::mapToDomain) }

    override suspend fun addWidget(widgetId: WidgetId, sheetId: SheetId) = withContext(ioDispatcher) {
        database.insert(widgetMapper.mapToDb(widgetId, sheetId))
    }

    override suspend fun deleteWidget(widgetId: WidgetId) = withContext(ioDispatcher) {
        database.delete(widgetId.value)
    }
}
