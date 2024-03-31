package com.example.words.widget.configuration

import android.appwidget.AppWidgetManager
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.example.words.repository.SheetsProvider
import com.example.words.settings.settingsDataStore
import com.example.words.ui.theme.GlanceWordsTheme
import kotlinx.coroutines.launch

class WidgetConfigurationActivity : ComponentActivity() {

    private val viewModel by viewModels<WidgetConfigurationViewModel>(factoryProducer = { WidgetConfigurationViewModel.factory(settingsDataStore) })
    private var clipboardChecked = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(TRANSPARENT))
        setResult(RESULT_CANCELED)
        super.onCreate(savedInstanceState)

        lifecycleScope.launch { SheetsProvider.initialize(assets::open) }

        val appWidgetId = getWidgetId() ?: run { finish(); return }

        setContent {
            GlanceWordsTheme {
                WidgetConfigurationScreen(
                    state = viewModel.state.collectAsState().value,
                    onCreateWidgetClick = {
                        lifecycleScope.launch {
                            viewModel.saveWidgetConfiguration(appWidgetId)
                            finishSuccessfully(appWidgetId)
                        }
                    },
                    onDismiss = ::finish,
                    onSheetSelect = viewModel::onSheetSelect,
                    onSpreadsheetIdChange = viewModel::onSpreadsheetIdChanged
                )
            }
        }
    }

    // Clipboard is available only when app is focused
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus && !clipboardChecked) {
            clipboardChecked = true
            setInitialSpreadsheetIdFromClipboard()
        }
    }

    private fun setInitialSpreadsheetIdFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text?.let {
            viewModel.setInitialSpreadsheetId(clipboardText = it)
        }
    }

    private fun getWidgetId(): Int? = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)?.takeIf { it != 0 }

    private fun finishSuccessfully(appWidgetId: Int) {
        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
