package com.example.words.widget.ui.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.words.di.DependencyContainer
import com.example.words.widget.WordsGlanceWidget

/*
 * Reshuffle as an action callback instead of a lambda to prevent multiple widget recompositions when widget is recreated.
 */
class ReshuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        (context.applicationContext as DependencyContainer).reshuffleNotifier.emitReshuffle()
        WordsGlanceWidget().update(context, glanceId)
    }
}