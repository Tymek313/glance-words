package com.pt.glancewords.widget.configuration

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pt.glancewords.R
import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SpreadsheetRepository
import com.pt.glancewords.domain.usecase.AddWidget
import com.pt.glancewords.logging.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(
    private val spreadsheetRepository: SpreadsheetRepository,
    private val addWidget: AddWidget,
    private val logger: Logger
) : ViewModel() {

    private val _state = MutableStateFlow(WidgetConfigurationState())
    val state: StateFlow<WidgetConfigurationState> = _state

    private var loadSheetsJob: Job? = null

    fun setInitialSpreadsheetIdIfApplicable(url: CharSequence) {
        val spreadsheetId = SPREADSHEET_URL_REGEX.find(url)?.groupValues?.get(1)
        if (spreadsheetId != null) {
            _state.update { it.copy(spreadsheetId = spreadsheetId) }
            loadSheetsForSpreadsheet(withDebounce = false)
        }
    }

    fun onSpreadsheetIdChanged(spreadsheetId: String) {
        _state.update { it.copy(spreadsheetId = spreadsheetId, spreadsheetError = null) }
        if (spreadsheetId.isNotBlank()) {
            loadSheetsForSpreadsheet(withDebounce = true)
        }
    }

    private fun loadSheetsForSpreadsheet(withDebounce: Boolean) {
        loadSheetsJob?.cancel()
        loadSheetsJob = viewModelScope.launch {
            if (withDebounce) {
                delay(SHEET_LOAD_DEBOUNCE)
            }
            _state.update { it.copy(isLoading = true, sheets = emptyList(), selectedSheetId = null) }
            val sheets = spreadsheetRepository.fetchSpreadsheetSheets(_state.value.spreadsheetId)
            _state.update { state ->
                if (sheets == null) {
                    state.copy(isLoading = false, spreadsheetError = R.string.could_not_download_sheets)
                } else {
                    state.copy(isLoading = false, sheets = sheets.map { WidgetConfigurationState.Sheet(it.id, it.name) })
                }
            }
        }
    }

    fun onSheetSelect(sheetId: Int) {
        _state.update { it.copy(selectedSheetId = sheetId) }
    }

    fun saveWidgetConfiguration(widgetId: Int) {
        val selectedSheetId = state.value.selectedSheetId ?: return run {
            logger.e(tag = javaClass.name, message = "Unknown selected sheet id")
        }
        _state.update { it.copy(isSavingWidget = true, generalError = null) }
        viewModelScope.launch {
            val widgetAdded = addWidget(WidgetId(widgetId), createSheetToAdd(selectedSheetId))
            if (widgetAdded) {
                _state.update { it.copy(widgetConfigurationSaved = true) }
            } else {
                _state.update { it.copy(isSavingWidget = false, generalError = R.string.could_not_synchronize_words) }
            }
        }
    }

    private fun createSheetToAdd(selectedSheetId: Int): NewSheet {
        val state = state.value
        return NewSheet(
            sheetSpreadsheetId = SheetSpreadsheetId(spreadsheetId = state.spreadsheetId, sheetId = selectedSheetId),
            name = state.sheets.first { it.id == selectedSheetId }.name
        )
    }

    companion object {
        private val SPREADSHEET_URL_REGEX = "https://docs.google.com/spreadsheets/d/([^/]+)".toRegex()
        private const val SHEET_LOAD_DEBOUNCE = 2000L
    }
}

data class WidgetConfigurationState(
    val spreadsheetId: String = "",
    val isLoading: Boolean = false,
    val isSavingWidget: Boolean = false,
    @StringRes val spreadsheetError: Int? = null,
    @StringRes val generalError: Int? = null,
    val sheets: List<Sheet> = emptyList(),
    val selectedSheetId: Int? = null,
    val widgetConfigurationSaved: Boolean = false
) {
    data class Sheet(val id: Int, val name: String)
}
