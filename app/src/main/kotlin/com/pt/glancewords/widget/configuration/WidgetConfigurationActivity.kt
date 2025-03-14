package com.pt.glancewords.widget.configuration

import android.appwidget.AppWidgetManager
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pt.glancewords.logging.Logger
import com.pt.glancewords.ui.theme.GlanceWordsTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetConfigurationActivity : ComponentActivity() {

    private val logger: Logger by inject()
    private val viewModel by viewModel<WidgetConfigurationViewModel>()
    private var clipboardChecked = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboardChecked = savedInstanceState?.getBoolean(KEY_CLIPBOARD_CHECKED) ?: false

        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(TRANSPARENT))
        setResult(RESULT_CANCELED)

        val appWidgetId = getWidgetId() ?: run {
            logger.e(javaClass.name, "Unknown widget id")
            finish()
            return
        }

        observeConfigurationFinish(appWidgetId)

        setContent {
            GlanceWordsTheme {
                WidgetConfigurationScreen(
                    state = viewModel.state.collectAsStateWithLifecycle().value,
                    onCreateWidgetClick = { viewModel.saveWidgetConfiguration(appWidgetId) },
                    onDismiss = ::finish,
                    onSheetSelect = viewModel::onSheetSelect,
                    onSpreadsheetIdChange = viewModel::onSpreadsheetIdChanged
                )
            }
        }
    }

    private fun observeConfigurationFinish(appWidgetId: Int) {
        viewModel.state
            .flowWithLifecycle(lifecycle)
            .filter { it.widgetConfigurationSaved }
            .onEach { finishSuccessfully(appWidgetId) }
            .launchIn(lifecycleScope)
    }

    // Clipboard is available only when app is focused
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !clipboardChecked) {
            clipboardChecked = true
            setInitialSpreadsheetIdFromClipboard()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CLIPBOARD_CHECKED, clipboardChecked)
    }

    private fun setInitialSpreadsheetIdFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text?.let {
            viewModel.setInitialSpreadsheetIdIfApplicable(url = it)
        }
    }

    private fun getWidgetId(): Int? = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)?.takeIf { it != 0 }

    private fun finishSuccessfully(appWidgetId: Int) {
        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        private const val KEY_CLIPBOARD_CHECKED = "clipboardChecked"
    }
}
