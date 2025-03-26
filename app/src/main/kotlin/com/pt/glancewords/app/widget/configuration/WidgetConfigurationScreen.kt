package com.pt.glancewords.app.widget.configuration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pt.glancewords.app.R
import com.pt.glancewords.app.ui.theme.GlanceWordsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigurationScreen(
    state: WidgetConfigurationState,
    onCreateWidgetClick: () -> Unit,
    onDismiss: () -> Unit,
    onSheetSelect: (sheetId: Int) -> Unit,
    onSpreadsheetIdChange: (sheetId: String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(Unit) {
        delay(500)
        if (currentState.spreadsheetId.isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(rememberSheetState(onDismiss)),
        sheetDragHandle = null,
        sheetContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                OutlinedTextField(
                    value = state.spreadsheetId,
                    onValueChange = onSpreadsheetIdChange,
                    label = { Text(text = stringResource(R.string.spreadsheet_id)) },
                    trailingIcon = { if (state.isLoading) CircularProgressIndicator(Modifier.padding(8.dp)) },
                    isError = state.spreadsheetError != null,
                    supportingText = state.spreadsheetError?.let { { Text(text = stringResource(it)) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                AnimatedVisibility(visible = state.sheets.isNotEmpty()) {
                    SheetList(sheets = state.sheets, selectedSheetId = state.selectedSheetId, onSheetSelect = onSheetSelect)
                }
                AnimatedVisibility(visible = state.generalError != null) {
                    Text(
                        text = state.generalError?.let { stringResource(it) }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = onCreateWidgetClick,
                    enabled = state.selectedSheetId != null && !state.isSavingWidget,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.create_widget),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        if (state.isSavingWidget) {
                            CircularProgressIndicator(
                                color = LocalContentColor.current,
                                strokeWidth = 3.dp,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        content = {},
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun rememberSheetState(onDismiss: () -> Unit): SheetState {
    val density = LocalDensity.current
    val state = SheetState(
        skipPartiallyExpanded = true,
        density = density,
        initialValue = SheetValue.Expanded
    )
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(Unit) {
        snapshotFlow { state.isVisible }.filterNot { it }.collect { currentOnDismiss() }
    }
    return remember { state }
}

@Composable
private fun SheetList(sheets: List<WidgetConfigurationState.Sheet>, selectedSheetId: Int?, onSheetSelect: (sheetId: Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(state = rememberScrollState())
    ) {
        sheets.forEach { sheet ->
            FilterChip(selected = sheet.id == selectedSheetId, onClick = { onSheetSelect(sheet.id) }, label = { Text(text = sheet.name) })
        }
    }
}

@Composable
@Preview
private fun ConfigureScreenPreview() {
    GlanceWordsTheme {
        WidgetConfigurationScreen(
            state = WidgetConfigurationState(
                spreadsheetId = "",
                sheets = listOf(
                    WidgetConfigurationState.Sheet(id = 1, name = "Sheet 1"),
                    WidgetConfigurationState.Sheet(id = 2, name = "Sheet 2")
                ),
                selectedSheetId = null,
                spreadsheetError = R.string.could_not_download_sheets,
                isLoading = true,
                isSavingWidget = true,
                widgetConfigurationSaved = false,
                generalError = R.string.could_not_synchronize_words
            ),
            onCreateWidgetClick = {},
            onDismiss = {},
            onSheetSelect = {},
            onSpreadsheetIdChange = {}
        )
    }
}

@Composable
@Preview
private fun ConfigureScreenSelectedSheetPreview() {
    GlanceWordsTheme {
        WidgetConfigurationScreen(
            state = WidgetConfigurationState(
                spreadsheetId = "",
                sheets = listOf(
                    WidgetConfigurationState.Sheet(id = 1, name = "Sheet 1"),
                    WidgetConfigurationState.Sheet(id = 2, name = "Sheet 2")
                ),
                selectedSheetId = 1,
                spreadsheetError = null,
                isLoading = false,
                isSavingWidget = false,
                widgetConfigurationSaved = false,
                generalError = null
            ),
            onCreateWidgetClick = {},
            onDismiss = {},
            onSheetSelect = {},
            onSpreadsheetIdChange = {}
        )
    }
}
