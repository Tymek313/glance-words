package com.example.glancewords.widget.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.glancewords.repository.SpreadsheetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ConfigureViewModel(private val spreadsheetRepository: SpreadsheetRepository): ViewModel() {

    private val _state = MutableStateFlow(ConfigureWidgetState.initial)
    val state: StateFlow<ConfigureWidgetState> = _state

    fun loadSpreadsheet(spreadsheetId: String) {
        // TODO
    }

    fun onSheetSelect(sheetId: String) {
        _state.update { it.copy(selectedSheetId = sheetId) }
    }

    companion object {
        val Factory = object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ConfigureViewModel(SpreadsheetRepository) as T
        }
    }
}

data class ConfigureWidgetState(
    val sheets: List<Sheet>?,
    val selectedSheetId: String?
) {
    class Sheet(val id: String, val name: String)

    companion object {
        val initial = ConfigureWidgetState(sheets = null, selectedSheetId = null)
    }
}