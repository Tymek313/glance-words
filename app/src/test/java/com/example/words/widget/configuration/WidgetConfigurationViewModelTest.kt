package com.example.words.widget.configuration

import com.example.words.coroutines.MainDispatcherRule
import com.example.words.coroutines.collectToListInBackground
import com.example.words.fixture.randomInt
import com.example.words.fixture.randomSpreadsheetSheet
import com.example.words.fixture.randomString
import com.example.words.fixture.sheetSpreadsheetIdFixture
import com.example.words.fixture.spreadsheetSheetForNewSheetFixture
import com.example.words.fixture.widgetWithExistingSheetFixture
import com.example.words.fixture.widgetWithNewSheetFixture
import com.example.words.logging.Logger
import com.example.words.model.Widget
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetRepository
import com.example.words.synchronization.WordsSynchronizer
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
    private val everySynchronizeWords get() = coEvery { fakeWordsSynchronizer.synchronizeWords(widgetWithExistingSheetFixture.id) }
    private fun everyFetchSpreadsheetSheets(spreadsheetId: String = SPREADSHEET_ID_FIXTURE) = coEvery {
        fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId)
    }

    private fun spreadsheetsFetchIsSuspended() = everyFetchSpreadsheetSheets() just awaits
    private fun spreadsheetsAreFetched() = everyFetchSpreadsheetSheets() returns listOf(spreadsheetSheetForNewSheetFixture)
    private fun spreadsheetsFetchFails() = everyFetchSpreadsheetSheets() throws Exception(FETCH_EXCEPTION_MESSAGE_FIXTURE)
    private fun addWidgetIsSuspended() = everyAddWidget just awaits
    private fun addWidgetFails() = everyAddWidget throws Exception(ADD_WIDGET_EXCEPTION_MESSAGE_FIXTURE)
    private fun widgetIsAdded() = everyAddWidget returns widgetWithExistingSheetFixture
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
        coEvery { fakeLogger.e(any(), any(), any()) } just runs
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

        viewModel.setInitialSpreadsheetIdIfApplicable(createValidUrl())

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID_FIXTURE, isLoading = true),
            viewModel.state.value
        )
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets has been fetched_state contains spreadsheet id and sheets and doesn't indicate loading`() {
        spreadsheetsAreFetched()

        viewModel.setInitialSpreadsheetIdIfApplicable(createValidUrl())

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID_FIXTURE, sheets = widgetSheetsFixture, isLoading = false),
            viewModel.state.value
        )
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets fetch fails_state contains spreadsheet id and spreadsheet exception message and doesn't indicate loading`() {
        spreadsheetsFetchFails()

        viewModel.setInitialSpreadsheetIdIfApplicable(createValidUrl())

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID_FIXTURE, isLoading = false, spreadsheetError = FETCH_EXCEPTION_MESSAGE_FIXTURE),
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

        viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID_FIXTURE)
        advanceUntilIdle()

        assertEquals(
            WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID_FIXTURE, isLoading = true),
            viewModel.state.value
        )
    }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets have been fetched_state spreadsheet id is updated and contains sheets and does not indicate loading`() =
        runTest {
            spreadsheetsAreFetched()

            viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID_FIXTURE)
            advanceUntilIdle()

            assertEquals(
                WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID_FIXTURE, isLoading = false, sheets = widgetSheetsFixture),
                viewModel.state.value
            )
        }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets fetch fails_state contains new spreadsheet id and error message and does not indicate loading`() =
        runTest {
            spreadsheetsFetchFails()

            viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID_FIXTURE)
            advanceUntilIdle()

            assertEquals(
                WidgetConfigurationState(spreadsheetId = SPREADSHEET_ID_FIXTURE, isLoading = false, spreadsheetError = FETCH_EXCEPTION_MESSAGE_FIXTURE),
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
            stateAfterSheetSelection.copy(isSavingWidget = true),
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
            stateAfterSheetSelection.copy(generalError = null, isSavingWidget = true),
            states.last()
        )
    }

    @Test
    fun `when saving widget configuration_given it succeeds_widget is stored in the repository`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
        wordsAreSynchronized()
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(widgetWithNewSheetFixture.id.value)

        coVerify {
            fakeWidgetRepository.addWidget(widgetWithNewSheetFixture.withSpreadsheetId(SPREADSHEET_ID_FIXTURE))
        }
    }

    @Test
    fun `when saving widget configuration_given it succeeds_words are synchronized`() = runTest(dispatcher) {
        spreadsheetsAreFetched()
        widgetIsAdded()
        wordsAreSynchronized()
        setSpreadsheetIdAndSheet()

        viewModel.saveWidgetConfiguration(randomInt())

        coVerify { fakeWordsSynchronizer.synchronizeWords(widgetWithExistingSheetFixture.id) }
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
            stateAfterSheetSelection.copy(
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
            stateAfterSheetSelection.copy(
                isSavingWidget = false,
                generalError = ADD_WIDGET_EXCEPTION_MESSAGE_FIXTURE,
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
        viewModel.onSpreadsheetIdChanged(SPREADSHEET_ID_FIXTURE)
        advanceUntilIdle()
        viewModel.onSheetSelect(sheetSpreadsheetIdFixture.sheetId)
    }

    private fun createValidUrl() = "https://docs.google.com/spreadsheets/d/$SPREADSHEET_ID_FIXTURE/"

    private fun Widget.withSpreadsheetId(spreadsheetId: String) = copy(
        sheet = sheet.copy(
            sheetSpreadsheetId = sheet.sheetSpreadsheetId.copy(spreadsheetId = spreadsheetId)
        )
    )

    private val stateAfterSheetSelection = WidgetConfigurationState(
        spreadsheetId = SPREADSHEET_ID_FIXTURE,
        selectedSheetId = spreadsheetSheetForNewSheetFixture.id,
        sheets = widgetSheetsFixture
    )

    companion object {
        private val widgetSheetsFixture =
            listOf(WidgetConfigurationState.Sheet(id = spreadsheetSheetForNewSheetFixture.id, name = spreadsheetSheetForNewSheetFixture.name))
        private val SPREADSHEET_ID_FIXTURE = randomString()
        private val FETCH_EXCEPTION_MESSAGE_FIXTURE = randomString()
        private val ADD_WIDGET_EXCEPTION_MESSAGE_FIXTURE = randomString()
    }
}