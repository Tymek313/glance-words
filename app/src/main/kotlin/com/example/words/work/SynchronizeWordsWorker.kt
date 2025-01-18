package com.example.words.work

import android.app.Notification
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.domain.model.Widget
import com.example.domain.synchronization.WordsSynchronizer
import com.example.words.logging.Logger
import com.example.words.notification.NotificationChannel
import com.example.words.notification.NotificationIds

class SynchronizeWordsWorker(
    private val context: Context,
    params: WorkerParameters,
    private val wordsSynchronizer: WordsSynchronizer,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(INPUT_WIDGET_ID, -1).takeIf { it != -1 }?.let(Widget::WidgetId)
        return if (widgetId == null) {
            logger.e(javaClass.name, "No widget id passed to the worker")
            Result.failure()
        } else try {
            wordsSynchronizer.synchronizeWords(widgetId)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NotificationIds.WIDGET_SYNCHRONIZATION,
            Notification.Builder(context, NotificationChannel.WIDGET_SYNCHRONIZATION.id).build()
        )
    }

    companion object {
        private const val INPUT_WIDGET_ID = "InputWidgetId"

        fun createInputData(appWidgetId: Int): Data {
            return Data.Builder().putInt(INPUT_WIDGET_ID, appWidgetId).build()
        }
    }
}