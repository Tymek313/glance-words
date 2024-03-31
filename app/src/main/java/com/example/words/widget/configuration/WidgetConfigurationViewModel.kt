package com.example.words.widget.configuration

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.words.Settings
import com.example.words.WidgetSettings
import com.example.words.repository.SheetsProvider
import com.example.words.repository.SpreadsheetRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(
    private val spreadsheetRepository: SpreadsheetRepository,
    private val dataStore: DataStore<Settings>
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigureWidgetState.initial)
    val state: StateFlow<ConfigureWidgetState> = _state

    private val loadSheetsExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(javaClass.name, null, throwable)
        _state.update { it.copy(spreadsheetError = throwable.localizedMessage, isLoading = false) }
    }

    private var loadSheetsJob: Job? = null

    fun setInitialSpreadsheetId(clipboardText: CharSequence) {
        _state.update { it.copy(spreadsheetId = SpreadsheetUrlRegex.find(clipboardText)?.groupValues?.get(1).orEmpty()) }
        loadSheetsForSpreadsheet(withDebounce = false)
    }

    fun onSpreadsheetIdChanged(spreadsheetId: String) {
        _state.update { it.copy(spreadsheetId = spreadsheetId, spreadsheetError = null) }
        if(spreadsheetId.isNotEmpty()) {
            loadSheetsForSpreadsheet(withDebounce = true)
        }
    }

    private fun loadSheetsForSpreadsheet(withDebounce: Boolean) {
        loadSheetsJob?.cancel()
        loadSheetsJob = viewModelScope.launch(loadSheetsExceptionHandler) {
            if(withDebounce) delay(2000)
            _state.update { it.copy(isLoading = true, sheets = null, selectedSheetId = null) }
            val sheets = spreadsheetRepository.fetchSpreadsheetSheets(_state.value.spreadsheetId)
            _state.update { state -> state.copy(isLoading = false, sheets = sheets.map { ConfigureWidgetState.Sheet(it.id, it.name) }) }
        }
    }


    fun onSheetSelect(sheetId: Int) {
        _state.update { it.copy(selectedSheetId = sheetId) }
    }

    suspend fun saveWidgetConfiguration(widgetId: Int) {
        viewModelScope.launch {
            dataStore.updateData { settings ->
                settings.toBuilder().addWidgets(
                    WidgetSettings.newBuilder()
                        .setWidgetId(widgetId)
                        .setSpreadsheetId(state.value.spreadsheetId)
                        .setSheetId(state.value.selectedSheetId!!)
                        .build()
                ).build()
            }
        }.join()
    }

    companion object {
        private val SpreadsheetUrlRegex = "https://docs.google.com/spreadsheets/d/(.+)/".toRegex()
        fun factory(dataStore: DataStore<Settings>) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WidgetConfigurationViewModel(SpreadsheetRepository(SheetsProvider.sheets), dataStore) as T
        }
    }
}

data class ConfigureWidgetState(
    val spreadsheetId: String,
    val isLoading: Boolean,
    val spreadsheetError: String?,
    val sheets: List<Sheet>?,
    val selectedSheetId: Int?,
) {
    class Sheet(val id: Int, val name: String)

    companion object {
        val initial = ConfigureWidgetState(
            spreadsheetId = "",
            sheets = null,
            selectedSheetId = null,
            spreadsheetError = null,
            isLoading = false
        )
    }
}