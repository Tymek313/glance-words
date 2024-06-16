package com.example.words.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.words.work.SynchronizeWordsWorker

// Normally we should have a lambda callback but it immediately causes the widget to recreate if the process has ended
// This means that the widget will be blinking. First, it would show loading state then load cached words and then show fresh synchronized words.
// Passing some flag to the `GlanceAppWidget` won't work because it's not recreated if the glance work is running. `GlanceWidgetManager.update()` won't help either
class LaunchWidgetSynchronizationWorkAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WorkManager.getInstance(context).enqueueUniqueWork(
            "WordsWidgetSynchronization-$appWidgetId",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SynchronizeWordsWorker>()
                .setInputData(SynchronizeWordsWorker.createInputData(appWidgetId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build(),
        )
    }
}