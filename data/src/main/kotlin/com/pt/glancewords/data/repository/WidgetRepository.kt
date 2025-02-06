package com.pt.glancewords.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.pt.glancewords.data.mapper.WidgetMapper
import com.pt.glancewords.database.DbWidgetQueries
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.WidgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultWidgetRepository(
    private val database: DbWidgetQueries,
    private val widgetMapper: WidgetMapper,
    private val ioDispatcher: CoroutineDispatcher
) : WidgetRepository {

    override fun observeWidget(widgetId: WidgetId): Flow<Widget?> {
        return database.getById(widgetId.value)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { dbWidget -> dbWidget?.let(widgetMapper::mapToDomain) }
    }

    override suspend fun addWidget(widgetId: WidgetId, sheetId: SheetId) = withContext(ioDispatcher) {
        database.insert(widgetMapper.mapToDb(widgetId, sheetId))
    }

    override suspend fun deleteWidget(widgetId: WidgetId) = withContext(ioDispatcher) {
        database.delete(widgetId.value)
    }
}