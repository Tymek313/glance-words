package com.example.words.widget.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Sheet
import com.example.domain.model.SheetSpreadsheetId
import com.example.domain.model.Widget
import com.example.domain.repository.SpreadsheetRepository
import com.example.domain.repository.WidgetRepository
import com.example.domain.synchronization.WordsSynchronizer
import com.example.words.logging.Logger
import com.example.words.logging.e
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(
    private val spreadsheetRepository: SpreadsheetRepository,
    private val widgetRepository: WidgetRepository,
    private val wordsSynchronizer: WordsSynchronizer,
    private val logger: Logger
) : ViewModel() {

    private val _state = MutableStateFlow(WidgetConfigurationState())
    val state: StateFlow<WidgetConfigurationState> = _state

    private val loadSheetsExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e(this, throwable)
        _state.update { it.copy(spreadsheetError = throwable.localizedMessage, isLoading = false) }
    }

    private val generalCoroutineHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e(this, throwable)
        _state.update { it.copy(isSavingWidget = false, generalError = throwable.localizedMessage) }
    }

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
        loadSheetsJob = viewModelScope.launch(loadSheetsExceptionHandler) {
            if (withDebounce) delay(SHEET_LOAD_DEBOUNCE)
            _state.update { it.copy(isLoading = true, sheets = emptyList(), selectedSheetId = null) }
            val sheets = spreadsheetRepository.fetchSpreadsheetSheets(_state.value.spreadsheetId)
            _state.update { state ->
                state.copy(
                    isLoading = false,
                    sheets = sheets.map { WidgetConfigurationState.Sheet(it.id, it.name) }
                )
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
        viewModelScope.launch(generalCoroutineHandler) {
            val storedWidget = widgetRepository.addWidget(createWidget(widgetId, selectedSheetId))
            wordsSynchronizer.synchronizeWords(storedWidget.id)
            _state.update { it.copy(widgetConfigurationSaved = true) }
        }
    }

    private fun createWidget(widgetId: Int, selectedSheetId: Int): Widget {
        val state = state.value
        return Widget(
            id = Widget.WidgetId(widgetId),
            sheet = Sheet.createNew(
                sheetSpreadsheetId = SheetSpreadsheetId(spreadsheetId = state.spreadsheetId, sheetId = selectedSheetId),
                name = state.sheets.first { it.id == selectedSheetId }.name,
            )
        )
    }

    companion object {
        private val SPREADSHEET_URL_REGEX = "https://docs.google.com/spreadsheets/d/(.+)/".toRegex()
        private const val SHEET_LOAD_DEBOUNCE = 2000L
    }
}

data class WidgetConfigurationState(
    val spreadsheetId: String = "",
    val isLoading: Boolean = false,
    val isSavingWidget: Boolean = false,
    val spreadsheetError: String? = null,
    val generalError: String? = null,
    val sheets: List<Sheet> = emptyList(),
    val selectedSheetId: Int? = null,
    val widgetConfigurationSaved: Boolean = false
) {
    data class Sheet(val id: Int, val name: String)
}