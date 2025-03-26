package com.pt.glancewords.app.widget.ui.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.pt.glancewords.app.application.dependencyContainer
import com.pt.glancewords.app.widget.ReshuffleNotifier
import com.pt.glancewords.app.widget.WordsGlanceWidget
import org.koin.core.component.get

/*
 * Reshuffle as an action callback instead of a lambda to prevent multiple widget recompositions when widget is recreated.
 */
class ReshuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.dependencyContainer.get<ReshuffleNotifier>().emitReshuffle()
        WordsGlanceWidget().update(context, glanceId)
    }
}
