package com.example.words.widget.configuration

import com.example.words.coroutines.MainDispatcherRule
import com.example.words.coroutines.collectToListInBackground
import com.example.words.domain.WordsSynchronizer
import com.example.words.logging.Logger
import com.example.words.model.SpreadsheetSheet
import com.example.words.randomInt
import com.example.words.randomString
import com.example.words.randomWidgetWithExistingSheet
import com.example.words.randomWidgetWithNewSheet
import com.example.words.repository.SpreadsheetRepository
import com.example.words.repository.WidgetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetConfigurationViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private lateinit var viewModel: WidgetConfigurationViewModel
    private lateinit var fakeSpreadsheetRepository: SpreadsheetRepository
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeWordsSynchronizer: WordsSynchronizer
    private lateinit var fakeLogger: Logger

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
    }

    @Test
    fun `when created_empty state is emitted`() {
        assertEquals(ConfigureWidgetState(), viewModel.state.value)
    }

    @Test
    fun `when initial spreadsheet id is set_given incorrect url_state does not change`() {
        viewModel.setInitialSpreadsheetIdIfApplicable(randomString())

        assertEquals(ConfigureWidgetState(), viewModel.state.value)
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets are being fetched_state indicates loading and contains spreadsheet id`() {
        val spreadsheetId = randomString()
        val url = createValidUrl(spreadsheetId)
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId) } coAnswers { suspendCoroutine { } }

        viewModel.setInitialSpreadsheetIdIfApplicable(url)

        assertEquals(ConfigureWidgetState(spreadsheetId = spreadsheetId, isLoading = true), viewModel.state.value)
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets has been fetched_state contains spreadsheet id and sheets and doesn't indicate loading`() {
        val spreadsheetId = randomString()
        val url = createValidUrl(spreadsheetId)
        val expectedState = ConfigureWidgetState(
            spreadsheetId = spreadsheetId,
            sheets = widgetSheetsFixture,
            isLoading = false
        )
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId) } returns sheetsFixture

        viewModel.setInitialSpreadsheetIdIfApplicable(url)

        assertEquals(expectedState, viewModel.state.value)
    }

    @Test
    fun `when initial spreadsheet id is set_given correct url and sheets fetch fails_state contains spreadsheet id and spreadsheet exception message and doesn't indicate loading`() {
        val spreadsheetId = randomString()
        val exceptionMessage = randomString()
        val url = createValidUrl(spreadsheetId)
        val expectedState = ConfigureWidgetState(spreadsheetId = spreadsheetId, isLoading = false, spreadsheetError = exceptionMessage)
        coEvery { fakeLogger.e(any(), any(), any()) } just runs
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId) } throws Exception(exceptionMessage)

        viewModel.setInitialSpreadsheetIdIfApplicable(url)

        assertEquals(expectedState, viewModel.state.value)
    }

    @Test
    fun `when spreadsheet id changes_given it is blank_state spreadsheet id is updated and not loading sheets`() = runTest(dispatcher) {
        val spreadsheetId = ""
        val expectedState = ConfigureWidgetState(spreadsheetId = spreadsheetId, isLoading = false)

        viewModel.onSpreadsheetIdChanged(spreadsheetId)
        advanceUntilIdle()

        assertEquals(expectedState, viewModel.state.value)
    }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets are being fetched_state spreadsheet id is updated and indicates loading`() = runTest {
        val spreadsheetId = randomString()
        val expectedState = ConfigureWidgetState(spreadsheetId = spreadsheetId, isLoading = true)
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId) } coAnswers { suspendCoroutine { } }

        viewModel.onSpreadsheetIdChanged(spreadsheetId)
        advanceUntilIdle()

        assertEquals(expectedState, viewModel.state.value)
    }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets have been fetched_state spreadsheet id is updated and contains sheets and does not indicate loading`() =
        runTest {
            val spreadsheetId = randomString()
            val expectedState = ConfigureWidgetState(spreadsheetId = spreadsheetId, isLoading = false, sheets = widgetSheetsFixture)
            coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId) } returns sheetsFixture

            viewModel.onSpreadsheetIdChanged(spreadsheetId)
            advanceUntilIdle()

            assertEquals(expectedState, viewModel.state.value)
        }

    @Test
    fun `when spreadsheet id changes_given it is not blank and sheets fetch fails_state contains new spreadsheet id and error message and does not indicate loading`() =
        runTest {
            val spreadsheetId = randomString()
            val exceptionMessage = randomString()
            val expectedState = ConfigureWidgetState(spreadsheetId = spreadsheetId, isLoading = false, spreadsheetError = exceptionMessage)
            coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(spreadsheetId) } throws Exception(exceptionMessage)
            coEvery { fakeLogger.e(any(), any(), any()) } just runs

            viewModel.onSpreadsheetIdChanged(spreadsheetId)
            advanceUntilIdle()

            assertEquals(expectedState, viewModel.state.value)
        }

    @Test
    fun `when spreadsheet id changes_given it is not blank_sheet fetch is debounced`() = runTest(dispatcher) {
        val firstSpreadsheetId = randomString()
        val secondSpreadsheetId = randomString()
        val expectedLastState = ConfigureWidgetState(
            spreadsheetId = secondSpreadsheetId,
            isLoading = false,
            sheets = widgetSheetsFixture
        )
        val states = collectToListInBackground(viewModel.state)
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(firstSpreadsheetId) } returns listOf(
            SpreadsheetSheet(id = randomInt(), name = randomString())
        )
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(secondSpreadsheetId) } returns sheetsFixture
        viewModel.onSpreadsheetIdChanged(firstSpreadsheetId)

        advanceTimeBy(1999)
        viewModel.onSpreadsheetIdChanged(secondSpreadsheetId)
        advanceTimeBy(2001)

        assertEquals(4, states.size)
        assertEquals(expectedLastState, states.last())
    }

    @Test
    fun `when sheet is selected_state contains the selected sheet id`() = runTest(dispatcher) {
        val sheetId = randomInt()
        val expectedLastState = ConfigureWidgetState(selectedSheetId = sheetId)
        val states = collectToListInBackground(viewModel.state)

        viewModel.onSheetSelect(sheetId)

        assertEquals(expectedLastState, states.last())
    }

    @Test
    fun `when saving widget configuration_given no selected sheet_state does not change`() = runTest(dispatcher) {
        val expectedState = ConfigureWidgetState(isSavingWidget = false)
        val states = collectToListInBackground(viewModel.state)
        every { fakeLogger.e(any(), any()) } just runs

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(expectedState, states.single())
    }

    @Test
    fun `when saving widget configuration_given selected sheet and saving is in progress_state indicates saving`() = runTest(dispatcher) {
        val spreadsheetId = randomString()
        val selectedSheetId = sheetFixture.id
        val expectedState = ConfigureWidgetState(
            spreadsheetId = spreadsheetId,
            isSavingWidget = true,
            sheets = widgetSheetsFixture,
            selectedSheetId = selectedSheetId
        )
        val states = collectToListInBackground(viewModel.state)
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(any()) } returns sheetsFixture
        coEvery { fakeWidgetRepository.addWidget(any()) } coAnswers { suspendCoroutine { } }
        viewModel.onSpreadsheetIdChanged(spreadsheetId)
        advanceUntilIdle()
        viewModel.onSheetSelect(selectedSheetId)

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(expectedState, states.last())
    }

    @Test
    fun `when saving widget configuration_given selected sheet_state error is cleared`() = runTest(dispatcher) {
        val spreadsheetId = randomString()
        val selectedSheetId = sheetFixture.id
        val expectedState = ConfigureWidgetState(
            spreadsheetId = spreadsheetId,
            generalError = null,
            isSavingWidget = true,
            selectedSheetId = selectedSheetId,
            sheets = widgetSheetsFixture
        )
        val states = collectToListInBackground(viewModel.state)
        every { fakeLogger.e(any(), any(), any()) } just runs
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(any()) } returns sheetsFixture
        coEvery { fakeWidgetRepository.addWidget(any()) } throws Exception() coAndThen { suspendCoroutine { } }
        viewModel.onSpreadsheetIdChanged(spreadsheetId)
        advanceUntilIdle()
        viewModel.onSheetSelect(selectedSheetId)
        viewModel.saveWidgetConfiguration(randomInt())

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(expectedState, states.last())
    }

    @Test
    fun `when saving widget configuration_given it succeeds_widget is stored in the repository`() = runTest(dispatcher) {
        val expectedWidgetToStore = randomWidgetWithNewSheet().run {
            copy(
                sheet = sheet.copy(
                    name = sheetFixture.name,
                    sheetSpreadsheetId = sheet.sheetSpreadsheetId.copy(sheetId = sheetFixture.id),
                )
            )
        }
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(any()) } returns sheetsFixture
        coEvery { fakeWidgetRepository.addWidget(any()) } returns expectedWidgetToStore
        coEvery { fakeWordsSynchronizer.synchronizeWords(any()) } just runs
        viewModel.onSpreadsheetIdChanged(expectedWidgetToStore.sheet.sheetSpreadsheetId.spreadsheetId)
        advanceUntilIdle()
        viewModel.onSheetSelect(sheetFixture.id)

        viewModel.saveWidgetConfiguration(expectedWidgetToStore.id.value)

        coVerify { fakeWidgetRepository.addWidget(expectedWidgetToStore) }
    }

    @Test
    fun `when saving widget configuration_given it succeeds_words are synchronized`() = runTest(dispatcher) {
        val storedWidget = randomWidgetWithNewSheet()
        val storedWidgetId = storedWidget.id
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(any()) } returns sheetsFixture
        coEvery { fakeWidgetRepository.addWidget(any()) } returns storedWidget
        coEvery { fakeWordsSynchronizer.synchronizeWords(storedWidgetId) } just runs
        viewModel.onSpreadsheetIdChanged(randomString())
        advanceUntilIdle()
        viewModel.onSheetSelect(sheetFixture.id)

        viewModel.saveWidgetConfiguration(randomInt())

        coVerifyAll { fakeWordsSynchronizer.synchronizeWords(storedWidgetId) }
    }

    @Test
    fun `when saving widget configuration_given it succeeds_state indicates success`() = runTest(dispatcher) {
        val spreadsheetId = randomString()
        val selectedSheet = sheetFixture.id
        val expectedState = ConfigureWidgetState(
            spreadsheetId = spreadsheetId,
            isSavingWidget = true,
            widgetConfigurationSaved = true,
            selectedSheetId = selectedSheet,
            sheets = widgetSheetsFixture
        )
        val states = collectToListInBackground(viewModel.state)
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(any()) } returns sheetsFixture
        coEvery { fakeWidgetRepository.addWidget(any()) } returns randomWidgetWithExistingSheet()
        coEvery { fakeWordsSynchronizer.synchronizeWords(any()) } just runs
        viewModel.onSpreadsheetIdChanged(spreadsheetId)
        advanceUntilIdle()
        viewModel.onSheetSelect(selectedSheet)

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(expectedState, states.last())
    }

    @Test
    fun `when saving widget configuration_given adding widget fails_state does not indicate saving and contains error`() = runTest(dispatcher) {
        val spreadsheetId = randomString()
        val selectedSheet = sheetFixture.id
        val errorMessage = randomString()
        val expectedState = ConfigureWidgetState(
            isSavingWidget = false,
            generalError = errorMessage,
            spreadsheetId = spreadsheetId,
            selectedSheetId = selectedSheet,
            sheets = widgetSheetsFixture
        )
        val states = collectToListInBackground(viewModel.state)
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(any()) } returns sheetsFixture
        coEvery { fakeWidgetRepository.addWidget(any()) } throws Exception(errorMessage)
        coEvery { fakeLogger.e(any(), any(), any()) } just runs
        viewModel.onSpreadsheetIdChanged(spreadsheetId)
        advanceUntilIdle()
        viewModel.onSheetSelect(selectedSheet)

        viewModel.saveWidgetConfiguration(randomInt())

        assertEquals(expectedState, states.last())
    }

    @Test
    fun `when saving widget configuration_given cannot find selected sheet in state_state does not indicate saving and contains error`() = runTest(dispatcher) {
        val states = collectToListInBackground(viewModel.state)
        coEvery { fakeSpreadsheetRepository.fetchSpreadsheetSheets(any()) } returns sheetsFixture
        coEvery { fakeLogger.e(any(), any(), any()) } just runs
        viewModel.onSpreadsheetIdChanged(randomString())
        advanceUntilIdle()
        viewModel.onSheetSelect(randomInt())

        viewModel.saveWidgetConfiguration(randomInt())

        assertFalse(states.last().generalError!!.isEmpty())
        assertFalse(states.last().isSavingWidget)
    }

    companion object {
        private val sheetsFixture: List<SpreadsheetSheet>
        private val widgetSheetsFixture: List<ConfigureWidgetState.Sheet>
        private val sheetFixture: SpreadsheetSheet

        init {
            val sheetId1 = randomInt()
            val sheetName1 = randomString()
            sheetFixture = SpreadsheetSheet(id = sheetId1, name = sheetName1)
            sheetsFixture = listOf(sheetFixture)
            widgetSheetsFixture = listOf(ConfigureWidgetState.Sheet(id = sheetId1, name = sheetName1))
        }

        fun createValidUrl(spreadsheetId: String) = "https://docs.google.com/spreadsheets/d/$spreadsheetId/"
    }
}