package com.example.glancewords.widget.configuration

import android.appwidget.AppWidgetManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.glancewords.widget.WordsGlanceWidget
import kotlinx.coroutines.launch

class WordsWidgetConfigureActivity : ComponentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setResult(RESULT_CANCELED)

        val appWidgetId = getWidgetId() ?: run { finish(); return }

        setContent {
            ConfigureScreen(
                onCreateWidgetClick = {
                    lifecycleScope.launch {
                        updateWidget(appWidgetId)
                        finishSuccessfully(appWidgetId)
                    }
                },
                onDismiss = ::finish
            )
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

private val spreadsheetUrlRegex = "https://docs.google.com/spreadsheets/d/(.+)/".toRegex()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigureScreen(onCreateWidgetClick: () -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            scrimColor = Color.Transparent,
            windowInsets = WindowInsets(0),
            sheetState = remember { SheetState(initialValue = SheetValue.Expanded, skipPartiallyExpanded = false) }
        ) {
            val focusRequester = remember { FocusRequester() }
            val context = LocalContext.current
            var spreadsheetId by remember { mutableStateOf("") }
            val keyboard = LocalSoftwareKeyboardController.current

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboard!!.show()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val spreadsheetIdOrClipboardUri = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.let { uriFromClipboard ->
                    spreadsheetUrlRegex.find(uriFromClipboard)?.groupValues?.get(1) ?: uriFromClipboard
                }
                spreadsheetId = spreadsheetIdOrClipboardUri ?: " "
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .navigationBarsPadding()
            ) {
                OutlinedTextField(
                    value = spreadsheetId,
                    onValueChange = { spreadsheetId = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text(text = "Spreadsheet ID") }
                )
                Button(
                    onClick = onCreateWidgetClick,
                    enabled = spreadsheetId.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(text = "Create widget")
                }
            }
        }
    }
}

@Composable
@Preview
private fun ConfigureScreenPreview() {
    ConfigureScreen(onCreateWidgetClick = {}, onDismiss = {})
}