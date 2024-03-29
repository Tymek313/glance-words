package com.example.glancewords.widget.configuration

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.glancewords.ui.theme.GlanceWordsTheme
import com.example.glancewords.widget.WordsGlanceWidget
import kotlinx.coroutines.launch

class WordsWidgetConfigureActivity : ComponentActivity() {

    private val viewModel by viewModels<ConfigureViewModel>(factoryProducer = ConfigureViewModel::Factory)

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(TRANSPARENT))
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId = getWidgetId() ?: run { finish(); return }

        setContent {
            GlanceWordsTheme {
                ConfigureScreen(
                    state = viewModel.state.collectAsState().value,
                    onCreateWidgetClick = {
                        lifecycleScope.launch {
                            updateWidget(appWidgetId)
                            finishSuccessfully(appWidgetId)
                        }
                    },
                    onDismiss = ::finish,
                    onSheetSelect = viewModel::onSheetSelect
                )
            }
        }
    }

    private fun getWidgetId(): Int? = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)?.takeIf { it != 0 }

    private suspend fun updateWidget(appWidgetId: Int) {
        val widgetId = GlanceAppWidgetManager(this@WordsWidgetConfigureActivity).getGlanceIdBy(appWidgetId)
        WordsGlanceWidget().update(this@WordsWidgetConfigureActivity, widgetId)
    }

    private fun finishSuccessfully(appWidgetId: Int) {
        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
