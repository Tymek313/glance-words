package com.pt.glancewords.work

import android.app.Notification
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.synchronization.WordsSynchronizer
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.d
import com.pt.glancewords.logging.e
import com.pt.glancewords.notification.NotificationChannel
import com.pt.glancewords.notification.NotificationIds

class SynchronizeWordsWorker(
    private val context: Context,
    params: WorkerParameters,
    private val wordsSynchronizer: WordsSynchronizer,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(INPUT_WIDGET_ID, -1).takeIf { it != -1 }?.let(::WidgetId)
        return if (widgetId == null) {
            logger.e(this, "No widget id passed to the worker")
            Result.failure()
        } else {
            val syncSucceeded = wordsSynchronizer.synchronizeWords(widgetId)
            if (syncSucceeded) {
                Result.success()
            } else {
                logger.d(this, "Words synchronization failed")
                Result.failure()
            }
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