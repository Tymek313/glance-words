package com.example.words.widget.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.words.DependencyContainer
import com.example.words.logging.Logger
import com.example.words.logging.e
import com.example.words.model.Widget
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsSynchronizer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(
    private val spreadsheetRepository: SpreadsheetRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val wordsSynchronizer: WordsSynchronizer,
    private val logger: Logger
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigureWidgetState.INITIAL)
    val state: StateFlow<ConfigureWidgetState> = _state

    private val loadSheetsExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e(javaClass.name, throwable)
        _state.update { it.copy(spreadsheetError = throwable.localizedMessage, isLoading = false) }
    }

    private val generalCoroutineHandler = CoroutineExceptionHandler { _, throwable ->
        _state.update { it.copy(isSavingWidget = false, generalError = throwable.localizedMessage) }
    }

    private var loadSheetsJob: Job? = null

    fun setInitialSpreadsheetIdIfApplicable(clipboardText: CharSequence) {
        val spreadsheetId = SPREADSHEET_URL_REGEX.find(clipboardText)?.groupValues?.get(1)
        if(spreadsheetId != null) {
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
            if (withDebounce) delay(2000)
            _state.update { it.copy(isLoading = true, sheets = null, selectedSheetId = null) }
            val sheets = spreadsheetRepository.fetchSpreadsheetSheets(_state.value.spreadsheetId)
            _state.update { state -> state.copy(isLoading = false, sheets = sheets.map { ConfigureWidgetState.Sheet(it.id, it.name) }) }
        }
    }


    fun onSheetSelect(sheetId: Int) {
        _state.update { it.copy(selectedSheetId = sheetId) }
    }


    fun saveWidgetConfiguration(widgetId: Int) {
        _state.update { it.copy(isSavingWidget = true, generalError = null) }
        viewModelScope.launch(generalCoroutineHandler) {
            state.value.run {
                widgetSettingsRepository.addWidget(
                    Widget(
                        id = Widget.WidgetId(widgetId),
                        spreadsheetId = spreadsheetId,
                        sheetId = selectedSheetId!!,
                        sheetName = sheets?.first { it.id == selectedSheetId }?.name.orEmpty(),
                        lastUpdatedAt = null
                    )
                )
            }
            wordsSynchronizer.synchronizeWords(Widget.WidgetId(widgetId))
            _state.update { it.copy(widgetConfigurationSaved = true) }
        }
    }

    companion object {
        private val SPREADSHEET_URL_REGEX = "https://docs.google.com/spreadsheets/d/(.+)/".toRegex()
        fun factory(diContainer: DependencyContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = diContainer.run {
                WidgetConfigurationViewModel(spreadsheetRepository, widgetSettingsRepository, wordsSynchronizer, logger) as T
            }
        }
    }
}

data class ConfigureWidgetState(
    val spreadsheetId: String,
    val isLoading: Boolean,
    val isSavingWidget: Boolean,
    val spreadsheetError: String?,
    val generalError: String?,
    val sheets: List<Sheet>?,
    val selectedSheetId: Int?,
    val widgetConfigurationSaved: Boolean
) {
    class Sheet(val id: Int, val name: String)

    companion object {
        val INITIAL = ConfigureWidgetState(
            spreadsheetId = "",
            sheets = null,
            selectedSheetId = null,
            spreadsheetError = null,
            isLoading = false,
            isSavingWidget = false,
            widgetConfigurationSaved = false,
            generalError = null
        )
    }
}