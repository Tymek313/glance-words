package com.example.words.repository

import androidx.datastore.core.DataStore
import com.example.words.ProtoSettings
import com.example.words.ProtoWidgetSettings
import com.example.words.settings.WidgetSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

interface WidgetSettingsRepository {
    fun observeSettings(appWidgetId: Int): Flow<WidgetSettings?>

    suspend fun addWidget(widget: WidgetSettings)

    suspend fun updateLastUpdatedAt(widgetId: WidgetSettings.WidgetId, lastUpdatedAt: Instant)

    suspend fun deleteWidget(widgetId: WidgetSettings.WidgetId)
}

class DefaultWidgetSettingsRepository(private val dataStore: DataStore<ProtoSettings>) : WidgetSettingsRepository {

    override fun observeSettings(appWidgetId: Int): Flow<WidgetSettings?> {
        return dataStore.data.map { settings ->
            val protoWidgetSettings = settings.widgetsList.find { it.widgetId == appWidgetId }
            protoWidgetSettings?.run {
                WidgetSettings(
                    widgetId = WidgetSettings.WidgetId(appWidgetId),
                    spreadsheetId = spreadsheetId,
                    sheetId = sheetId,
                    sheetName = sheetName,
                    lastUpdatedAt = if(hasLastUpdatedAt()) Instant.ofEpochSecond(lastUpdatedAt) else null
                )
            }
        }
    }

    override suspend fun addWidget(widget: WidgetSettings) {
        val updatedSettings = ProtoWidgetSettings.newBuilder()
            .setWidgetId(widget.widgetId.value)
            .setSpreadsheetId(widget.spreadsheetId)
            .setSheetId(widget.sheetId)
            .setSheetName(widget.sheetName)
            .build()

        dataStore.updateData { protoSettings -> protoSettings.toBuilder().addWidgets(updatedSettings).build() }
    }

    override suspend fun updateLastUpdatedAt(widgetId: WidgetSettings.WidgetId, lastUpdatedAt: Instant) {
        dataStore.updateData { protoSettings ->
            val widgetIndex = protoSettings.widgetsList.indexOfFirst { it.widgetId == widgetId.value }
            val widget = protoSettings.getWidgets(widgetIndex)
            protoSettings.toBuilder()
                .setWidgets(widgetIndex, widget.toBuilder().setLastUpdatedAt(lastUpdatedAt.epochSecond).build())
                .build()
        }
    }

    override suspend fun deleteWidget(widgetId: WidgetSettings.WidgetId) {
        dataStore.updateData { settings ->
            settings.toBuilder()
                .removeWidgets(settings.widgetsList.indexOfFirst { it.widgetId == widgetId.value })
                .build()
        }
    }
}