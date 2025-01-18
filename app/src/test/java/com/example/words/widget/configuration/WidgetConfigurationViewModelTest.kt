package com.example.words.widget.configuration

import com.example.domain.model.Sheet
import com.example.domain.model.SheetId
import com.example.domain.model.SheetSpreadsheetId
import com.example.domain.model.SpreadsheetSheet
import com.example.domain.model.Widget
import com.example.domain.repository.SpreadsheetRepository
import com.example.domain.repository.WidgetRepository
import com.example.domain.synchronization.WordsSynchronizer
import com.example.testcommon.coroutines.collectToListInBackground
import com.example.testcommon.fixture.randomInstant
import com.example.testcommon.fixture.randomInt
import com.example.testcommon.fixture.randomString
import com.example.words.logging.Logger
import com.example.words.widget.coroutines.MainDispatcherRule
import io.mockk.andThenJust
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigurationViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private lateinit var viewModel: WidgetConfigurationViewModel
    private lateinit var fakeSpreadsheetRepository: SpreadsheetRepository
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeWordsSynchronizer: WordsSynchronizer
    private lateinit var fakeLogger: Logger

    private val everyAddWidget get() = coEvery { fakeWidgetRepository.addWidget(any()) }
    private val everySynchronizeWords get() = coEvery { fakeWordsSynchronizer.synchronizeWords(STORED_WIDGET.id) }
    private fun everyFetchSpreadsheetSheets(spreadsheetId: String = SPREADSHEET_ID) = coEvery {
        fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId)
    }

    private fun spreadsheetsFetchIsSuspended() = everyFetchSpreadsheetSheets() just awaits
    private fun spreadsheetsAreFetched() = everyFetchSpreadsheetSheets() returns listOf(FETCHED_SPREADSHEET_SHEET)
    private fun spreadsheetsFetchFails() = everyFetchSpreadsheetSheets() throws Exception(FETCH_EXCEPTION_MESSAGE)
    private fun addWidgetIsSuspended() = everyAddWidget just awaits
    private fun addWidgetFails() = everyAddWidget throws Exception(ADD_WIDGET_EXCEPTION_MESSAGE)
    private fun widgetIsAdded() = everyAddWidget returns STORED_WIDGET
    private fun wordsAreSynchronized() = everySynchronizeWords just runs

    @Before
    fun setup() {
        fakeSpreadsheetRepository = mockk()
        fakeWidgetRepository = mockk()
        fakeWordsSynchronizer = mockk()
        fakeLogger = mockk()
        viewModel = WidgetConfigurationViewModel(
            fakeSpreadsheetRepository,
            fakeWidgetRepository,
            fakeWordsSynchronizer,
            fakeLogger
        )
        every { fakeLogger.e(any(), any(), any()) } just runs
        every { fakeLogger.e(any(), any()) } just runs
    }

    @Test
    fun `when created_empty state is emitted`() {
        assertEquals(WidgetConfigurationState(), viewModel.state.value)
    }

    @Test
    fun `when initial spreadsheet id is set_given incorrect url_state does not change`() {
        viewModel.setInitialSpreadsheetIdIfApplicable(randomString())

        assertEquals(WidgetConfigurationState(), viewModel.state.value)
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets are being fetched_state indicates loading and contains spreadsheet id`() {
        spreadsheetsFetchIsSuspended()

        viewModel.setInitialSpreadsheetIdIfApplicable(VALID_URL)

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, isLoading = true),
            viewModel.state.value
        )
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets has been fetched_state contains spreadsheet id and sheets and doesn't indicate loading`() {
        spreadsheetsAreFetched()

        viewModel.setInitialSpreadsheetIdIfApplicable(VALID_URL)

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, sheets = SHEETS, isLoading = false),
            viewModel.state.value
        )
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets fetch fails_state contains spreadsheet id and spreadsheet exception message and doesn't indicate loading`() {
        spreadsheetsFetchFails()

        viewModel.setInitialSpreadsheetIdIfApplicable(VALID_URL)

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, isLoading = false, spreadsheetError = FETCH_EXCEPTION_MESSAGE),
            viewModel.state.value
        )
    }

    @Test
    fun `when spreadsheet id changes_given it is blank_state spreadsheet id is updated and not loading sheets`() = runTest(dispatcher) {
        viewModel.onSpreadsheetIdChanged("")
        advanceUntilIdle()

        assertEquals(WidgetConfigurationState(spreadsheetId = "", isLoading = false), viewModel.state.value)
    }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets are being fetched_state spreadsheet id is updated and indicates loading`() = runTest {
        spreadsheetsFetchIsSuspended()

        viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID)
        advanceUntilIdle()

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, isLoading = true),
            viewModel.state.value
        )
    }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets have been fetched_state spreadsheet id is updated and contains sheets and does not indicate loading`() =
        runTest {
            spreadsheetsAreFetched()

            viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID)
            advanceUntilIdle()

            assertEquals(
                WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, isLoading = false, sheets = SHEETS),
                viewModel.state.value
            )
        }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets fetch fails_state contains new spreadsheet id and error message and does not indicate loading`() =
        runTest {
            spreadsheetsFetchFails()

            viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID)
            advanceUntilIdle()

            assertEquals(
                WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, isLoading = false, spreadsheetError = FETCH_EXCEPTION_MESSAGE),
                viewModel.state.value
            )
        }

    @Test
    fun `when spreadsheet id changes_given it is not blank_sheet fetch is debounced`() = runTest(dispatcher) {
        val firstSpreadsheetId = randomString()
        val secondSpreadsheetId = randomString()
        val states = collectToListInBackground(viewModel.state)
        everyFetchSpreadsheetSheets(firstSpreadsheetId) returns listOf(randomSpreadsheetSheet())
        everyFetchSpreadsheetSheets(secondSpreadsheetId) returns listOf(randomSpreadsheetSheet())

        viewModel.onSpreadsheetIdChanged(firstSpreadsheetId)
        advanceTimeBy(1999)
        viewModel.onSpreadsheetIdChanged(secondSpreadsheetId)
        advanceTimeBy(2001)

        assertEquals(4, states.size)
    }

    @Test
    fun `when sheet is selected_state contains the selected sheet id`() = runTest(dispatcher) {
        val sheetId = randomInt()
        val states = collectToListInBackground(viewModel.state)

        viewModel.onSheetSelect(sheetId)

        assertEquals(WidgetConfigurationState(selectedSheetId = sheetId), states.last())
    }

    @Test
    fun `when saving widget configuration_given no selected sheet_state does not change`() = runTest(dispatcher) {
        val states = collectToListInBackground(viewModel.state)

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(WidgetConfigurationState(isSavingWidget = false), states.single())
    }

    @Test
    fun `when saving widget configuration_given selected sheet and saving is in progress_state indicates saving`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        addWidgetIsSuspended()
        val states = collectToListInBackground(viewModel.state)
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(
            STATE_AFTER_SHEET_SELECTION.copy(isSavingWidget = true),
            states.last()
        )
    }

    @Test
    fun `when saving widget configuration_given selected sheet_state error is cleared`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        addWidgetFails() andThenJust awaits
        setSpreadsheetIdAndSheet()
        val states = collectToListInBackground(viewModel.state)
        viewModel.saveWidgetConfiguration(randomInt())

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(
            STATE_AFTER_SHEET_SELECTION.copy(generalError = null, isSavingWidget = true),
            states.last()
        )
    }

    @Test
    fun `when saving widget configuration_given it succeeds_widget is stored in the repository`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
        wordsAreSynchronized()
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(WIDGET_TO_STORE.id.value)

        coVerify {
            fakeWidgetRepository.addWidget(WIDGET_TO_STORE)
        }
    }

    @Test
    fun `when saving widget configuration_given it succeeds_words are synchronized`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
        wordsAreSynchronized()
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(randomInt())

        coVerify { fakeWordsSynchronizer.synchronizeWords(STORED_WIDGET.id) }
    }

    @Test
    fun `when saving widget configuration_given it succeeds_state indicates success`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
        wordsAreSynchronized()
        val states = collectToListInBackground(viewModel.state)
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(
            STATE_AFTER_SHEET_SELECTION.copy(
                isSavingWidget = true,
                widgetConfigurationSaved = true,
            ),
            states.last()
        )
    }

    @Test
    fun `when saving widget configuration_given adding widget fails_state does not indicate saving and contains error`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        addWidgetFails()
        val states = collectToListInBackground(viewModel.state)
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(
            STATE_AFTER_SHEET_SELECTION.copy(
                isSavingWidget = false,
                generalError = ADD_WIDGET_EXCEPTION_MESSAGE,
            ),
            states.last()
        )
    }

    @Test
    fun `when saving widget configuration_given cannot find selected sheet in state_state does not indicate saving and contains error`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        val states = collectToListInBackground(viewModel.state)
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(randomInt())

        assertFalse(states.last().generalError!!.isEmpty(), "contains error")
        assertFalse(states.last().isSavingWidget, "indicates saving")
    }

    private fun TestScope.setSpreadsheetIdAndSheet() {
        viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID)
        advanceUntilIdle()
        viewModel.onSheetSelect(FETCHED_SPREADSHEET_SHEET.id)
    }

    private fun randomSpreadsheetSheet() = SpreadsheetSheet(id = randomInt(), name = randomString())

    private companion object {
        val SPREADSHEET_ID = randomString()
        val VALID_URL = "https://docs.google.com/spreadsheets/d/$SPREADSHEET_ID/"
        val FETCH_EXCEPTION_MESSAGE = randomString()
        val ADD_WIDGET_EXCEPTION_MESSAGE = randomString()
        val STORED_WIDGET = Widget(
            id = Widget.WidgetId(randomInt()),
            sheet = Sheet.createExisting(
                id = SheetId(randomInt()),
                sheetSpreadsheetId = SheetSpreadsheetId(randomString(), randomInt()),
                name = randomString(),
                lastUpdatedAt = randomInstant()
            )
        )
        val FETCHED_SPREADSHEET_SHEET = SpreadsheetSheet(id = randomInt(), name = randomString())
        val SHEETS = listOf(
            WidgetConfigurationState.Sheet(id = FETCHED_SPREADSHEET_SHEET.id, name = FETCHED_SPREADSHEET_SHEET.name)
        )
        val WIDGET_TO_STORE = Widget(
            id = Widget.WidgetId(randomInt()),
            sheet = Sheet.createNew(
                sheetSpreadsheetId = SheetSpreadsheetId(SPREADSHEET_ID, FETCHED_SPREADSHEET_SHEET.id),
                name = FETCHED_SPREADSHEET_SHEET.name
            )
        )
        val STATE_AFTER_SHEET_SELECTION = WidgetConfigurationState(
            spreadsheetId = SPREADSHEET_ID,
            selectedSheetId = FETCHED_SPREADSHEET_SHEET.id,
            sheets = SHEETS
        )
    }
}