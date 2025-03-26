package com.pt.glancewords.app.work

import android.app.Notification
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.pt.glancewords.app.notification.NotificationChannel
import com.pt.glancewords.app.notification.NotificationIds
import com.pt.glancewords.domain.widget.model.WidgetId
import com.pt.glancewords.domain.words.usecase.SynchronizeWords
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.logging.d
import com.pt.glancewords.logging.e

class SynchronizeWordsWorker(
    private val context: Context,
    params: WorkerParameters,
    private val synchronizeWords: SynchronizeWords,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(INPUT_WIDGET_ID, -1).takeIf { it != -1 }?.let(::WidgetId)
        return if (widgetId == null) {
            logger.e(this, "No widget id passed to the worker")
            Result.failure()
        } else {
            val syncSucceeded = synchronizeWords(widgetId)
            if (syncSucceeded) {
                Result.success()
            } else {
                logger.d(this, "Words synchronization failed")
                Result.failure()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NotificationIds.WIDGET_SYNCHRONIZATION,
        Notification.Builder(context, NotificationChannel.WIDGET_SYNCHRONIZATION.id).build()
    )

    companion object {
        private const val INPUT_WIDGET_ID = "InputWidgetId"

        fun createInputData(appWidgetId: Int): Data = Data.Builder().putInt(INPUT_WIDGET_ID, appWidgetId).build()
    }
}
