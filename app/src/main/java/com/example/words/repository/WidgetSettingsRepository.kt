package com.example.words.repository

import com.example.words.ProtoSettings
import com.example.words.ProtoWidget
import com.example.words.model.Widget
import com.example.words.persistence.Persistence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

interface WidgetSettingsRepository {
    fun observeSettings(widgetId: Widget.WidgetId): Flow<Widget?>

    suspend fun addWidget(widget: Widget)

    suspend fun updateLastUpdatedAt(widgetId: Widget.WidgetId, lastUpdatedAt: Instant)

    suspend fun deleteWidget(widgetId: Widget.WidgetId)
}

class DefaultWidgetSettingsRepository(private val persistence: Persistence<ProtoSettings>) : WidgetSettingsRepository {

    override fun observeSettings(widgetId: Widget.WidgetId): Flow<Widget?> {
        return persistence.data.map { settings ->
            val protoWidgetSettings = settings.widgetsList.find { it.id == widgetId.value }
            protoWidgetSettings?.run {
                Widget(
                    id = Widget.WidgetId(id),
                    spreadsheetId = spreadsheetId,
                    sheetId = sheetId,
                    sheetName = sheetName,
                    lastUpdatedAt = if(hasLastUpdatedAt()) Instant.ofEpochSecond(lastUpdatedAt) else null
                )
            }
        }
    }

    override suspend fun addWidget(widget: Widget) {
        val updatedSettings = ProtoWidget.newBuilder()
            .setId(widget.id.value)
            .setSpreadsheetId(widget.spreadsheetId)
            .setSheetId(widget.sheetId)
            .setSheetName(widget.sheetName)
            .build()

        persistence.updateData { protoSettings -> protoSettings.toBuilder().addWidgets(updatedSettings).build() }
    }

    override suspend fun updateLastUpdatedAt(widgetId: Widget.WidgetId, lastUpdatedAt: Instant) {
        persistence.updateData { protoSettings ->
            val widgetIndex = protoSettings.widgetsList
                .indexOfFirst { it.id == widgetId.value }
                .takeIf { it > -1 }
                ?: throw WidgetDoesNotExistException(widgetId)

            val widget = protoSettings.getWidgets(widgetIndex)
            protoSettings.toBuilder()
                .setWidgets(widgetIndex, widget.toBuilder().setLastUpdatedAt(lastUpdatedAt.epochSecond).build())
                .build()
        }
    }

    override suspend fun deleteWidget(widgetId: Widget.WidgetId) {
        persistence.updateData { settings ->
            settings.toBuilder()
                .removeWidgets(settings.widgetsList.indexOfFirst { it.id == widgetId.value })
                .build()
        }
    }

    class WidgetDoesNotExistException(widgetId: Widget.WidgetId): Exception("Widget ${widgetId.value} does not exist")
}