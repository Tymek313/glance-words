package com.pt.glancewords.widget.configuration

import com.pt.glancewords.R
import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.SpreadsheetSheet
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SpreadsheetRepository
import com.pt.glancewords.domain.usecase.AddWidget
import com.pt.glancewords.widget.coroutines.MainDispatcherRule
import com.pt.testcommon.coroutines.collectToListInBackground
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString
import io.mockk.andThenJust
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigurationViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private lateinit var viewModel: WidgetConfigurationViewModel
    private lateinit var fakeSpreadsheetRepository: SpreadsheetRepository
    private lateinit var fakeAddWidget: AddWidget

    private val everyAddWidget get() = coEvery { fakeAddWidget(any()) }
    private fun everyFetchSpreadsheetSheets(spreadsheetId: String = SPREADSHEET_ID) = coEvery {
        fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId)
    }

    private fun spreadsheetsFetchIsSuspended() = everyFetchSpreadsheetSheets() just awaits
    private fun spreadsheetsAreFetched() = everyFetchSpreadsheetSheets() returns listOf(FETCHED_SPREADSHEET_SHEET)
    private fun spreadsheetsFetchFails() = everyFetchSpreadsheetSheets() returns null
    private fun addWidgetIsSuspended() = everyAddWidget just awaits
    private fun widgetIsAdded() = everyAddWidget returns true
    private fun widgetAddFails() = everyAddWidget returns false

    @Before
    fun setup() {
        fakeSpreadsheetRepository = mockk()
        fakeAddWidget = mockk()
        viewModel = WidgetConfigurationViewModel(
            fakeSpreadsheetRepository,
            fakeAddWidget,
            mockk(relaxed = true)
        )
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
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, isLoading = false, spreadsheetError = R.string.could_not_download_sheets),
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
                WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID, isLoading = false, spreadsheetError = R.string.could_not_download_sheets),
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
    fun `when saving widget configuration_given previous widget addition failed_state error is cleared`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
        widgetAddFails() andThenJust awaits
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
    fun `when saving widget configuration_given widget addition succeeds_widget is stored in the repository`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(WIDGET_TO_ADD.widgetId.value)

        coVerify { fakeAddWidget.invoke(WIDGET_TO_ADD) }
    }

    @Test
    fun `when saving widget configuration_given widget addition succeeds_state indicates success`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
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
    fun `when saving widget configuration_given widget addition fails_state does not indicate saving and contains an error`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetAddFails()
        val states = collectToListInBackground(viewModel.state)
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(
            STATE_AFTER_SHEET_SELECTION.copy(
                isSavingWidget = false,
                generalError = R.string.could_not_synchronize_words,
            ),
            states.last()
        )
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
        val FETCHED_SPREADSHEET_SHEET = SpreadsheetSheet(id = randomInt(), name = randomString())
        val SHEETS = listOf(
            WidgetConfigurationState.Sheet(id = FETCHED_SPREADSHEET_SHEET.id, name = FETCHED_SPREADSHEET_SHEET.name)
        )
        val WIDGET_TO_ADD = AddWidget.WidgetToAdd(
            widgetId = WidgetId(randomInt()),
            sheet = NewSheet(
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