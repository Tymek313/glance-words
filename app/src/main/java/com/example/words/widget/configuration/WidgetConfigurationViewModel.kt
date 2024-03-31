package com.example.words.widget.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.words.repository.SheetsProvider
import com.example.words.repository.SpreadsheetRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(private val spreadsheetRepository: SpreadsheetRepository) : ViewModel() {

    private val _state = MutableStateFlow(ConfigureWidgetState.initial)
    val state: StateFlow<ConfigureWidgetState> = _state

    private var loadSheetsJob: Job? = null

    fun loadSheetsForSpreadsheet(spreadsheetId: String) {
        loadSheetsJob?.cancel()
        loadSheetsJob = viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable -> _state.update { it.copy(spreadsheetError = throwable.localizedMessage, isLoading = false) } }
        ) {
            delay(2000)
            _state.update { it.copy(isLoading = true, spreadsheetError = null, sheets = null, selectedSheetId = null) }
            val sheets = spreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId)
            _state.update { state -> state.copy(isLoading = false, sheets = sheets.map { ConfigureWidgetState.Sheet(it.id, it.name) }) }
        }
    }

    fun onSheetSelect(sheetId: Int) {
        _state.update { it.copy(selectedSheetId = sheetId) }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = WidgetConfigurationViewModel(SpreadsheetRepository(SheetsProvider.sheets)) as T
        }
    }
}

data class ConfigureWidgetState(
    val isLoading: Boolean,
    val spreadsheetError: String?,
    val sheets: List<Sheet>?,
    val selectedSheetId: Int?
) {
    class Sheet(val id: Int, val name: String)

    companion object {
        val initial = ConfigureWidgetState(sheets = null, selectedSheetId = null, spreadsheetError = null, isLoading = false)
    }
}