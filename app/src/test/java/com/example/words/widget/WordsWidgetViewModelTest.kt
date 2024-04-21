package com.example.words.widget

import com.example.words.logging.Logger
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer
import com.example.words.settings.WidgetSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.Random
import java.util.UUID
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@MockKExtension.ConfirmVerification
class WordsWidgetViewModelTest {

    private lateinit var viewModel: WordsWidgetViewModel
    private lateinit var mockWidgetSettingsRepository: WidgetSettingsRepository
    private lateinit var mockWordsSynchronizer: WordsSynchronizer
    private lateinit var mockWordsRepository: WordsRepository
    private lateinit var mockLogger: Logger

    @Before
    fun setUp() {
        mockWidgetSettingsRepository = mockk()
        mockWordsSynchronizer = mockk()
        mockWordsRepository = mockk()
        mockLogger = mockk()
        every { mockWidgetSettingsRepository.observeSettings(0) } returns flowOf(widgetSettingsFixture)
        every { mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture) } returns flowOf(wordsFixture)
    }

    @Test
    fun `given the widget has been updated_when view model is widget details state should contain correct state with filled last update date`() =
        runTest(UnconfinedTestDispatcher()) {
            every { mockWidgetSettingsRepository.observeSettings(appWidgetId = 0) } returns flowOf(
                widgetSettingsFixture.copy(lastUpdatedAt = Instant.parse("2024-04-21T19:00:00.00Z"))
            )
            viewModel = createViewModel()

            assertEquals(
                expected = WidgetDetailsState(sheetName = sheetNameFixture, lastUpdatedAt = "Apr 21, 2024, 7:00 PM"),
                actual = viewModel.widgetDetailsState.first()
            )
            verifyAll {
                mockWidgetSettingsRepository.observeSettings(appWidgetId = 0)
            }
        }

    @Test
    fun `given the widget has not been updated_when view model is widget details state should contain successful state with empty last update date`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = createViewModel()

            assertEquals(expected = WidgetDetailsState(sheetName = sheetNameFixture, lastUpdatedAt = ""), actual = viewModel.widgetDetailsState.first())
            verifyAll {
                mockWidgetSettingsRepository.observeSettings(appWidgetId = 0)
            }
        }

    @Test
    fun `given there are words_when view model is created_words state should contain successful state`() = runTest(UnconfinedTestDispatcher()) {
        viewModel = createViewModel()

        val states = collectWordsStates()

        assertEquals(expected = WidgetState.Success(wordsFixture), actual = states.last())
        verifyAll {
            mockWidgetSettingsRepository.observeSettings(appWidgetId = 0)
            mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture)
        }
    }

    @Test
    fun `given observing words fails_when view model is created_words state should contain failed state`() = runTest(UnconfinedTestDispatcher()) {
        every { mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture) } throws Exception("Boom!")
        every { mockLogger.e(any(), any(), any()) } just runs
        viewModel = createViewModel()

        val states = collectWordsStates()

        assertEquals(expected = WidgetState.Failure, actual = states.last())
        verifyAll {
            mockWidgetSettingsRepository.observeSettings(appWidgetId = 0)
            mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture)
        }
    }

    @Test
    fun `given there are no words_when view model is created_words state should contain failed state`() = runTest(UnconfinedTestDispatcher()) {
        every { mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture) } returns flowOf(null)
        viewModel = createViewModel()

        val states = collectWordsStates()

        assertEquals(expected = WidgetState.Failure, actual = states.last())
        verifyAll {
            mockWidgetSettingsRepository.observeSettings(appWidgetId = 0)
            mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture)
        }
    }

    @Test
    fun `when reshuffling words_words state should contain new set of words`() = runTest(UnconfinedTestDispatcher()) {
        var firstRun = true
        every { mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture) } answers {
            flowOf(
                if (firstRun) {
                    firstRun = false
                    wordsFixture
                } else {
                    otherWordsFixture
                }
            )
        }
        viewModel = createViewModel()

        val states = collectWordsStates()
        viewModel.reshuffleWords()

        assertEquals(expected = WidgetState.Success(otherWordsFixture), actual = states.last())
        verifyOrder {
            mockWidgetSettingsRepository.observeSettings(appWidgetId = 0)
            mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture)
        }
        verify(exactly = 2) { mockWordsRepository.observeRandomWords(spreadsheetIdFixture, sheetIdFixture) }
    }

    @Test
    fun `when synchronizing words_words state should be in progress`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { mockWordsSynchronizer.synchronizeWords(widgetId = 0) } just runs
        viewModel = createViewModel()

        val states = collectWordsStates()
        viewModel.synchronizeWords()

        assertEquals(expected = WidgetState.InProgress, actual = states.last())
        coVerify { mockWordsSynchronizer.synchronizeWords(widgetId = 0) }
    }

    @Test
    fun `when widget is deleted_its settings should be deleted`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { mockWidgetSettingsRepository.deleteWidget(WidgetSettings.WidgetId(0)) } just runs
        viewModel = createViewModel()

        viewModel.deleteWidget()

        coVerify { mockWidgetSettingsRepository.deleteWidget(widgetId = WidgetSettings.WidgetId(0)) }
    }

    private fun createViewModel() = WordsWidgetViewModel(
        appWidgetId = 0,
        widgetSettingsRepository = mockWidgetSettingsRepository,
        wordsSynchronizer = mockWordsSynchronizer,
        wordsRepository = mockWordsRepository,
        locale = Locale.US,
        zoneId = ZoneId.of("UTC"),
        logger = mockLogger
    )

    private fun TestScope.collectWordsStates(): List<WidgetState> {
        val states = mutableListOf<WidgetState>()
        backgroundScope.launch { viewModel.wordsState.toList(states) }
        return states
    }

    private companion object {
        val widgetIdFixture = Random().nextInt()
        val spreadsheetIdFixture = UUID.randomUUID().toString()
        val sheetIdFixture = Random().nextInt()
        val sheetNameFixture = UUID.randomUUID().toString()
        val wordsFixture = listOf("1" to "1")
        val otherWordsFixture = listOf("1" to "2")
        val widgetSettingsFixture = WidgetSettings(
            widgetId = WidgetSettings.WidgetId(widgetIdFixture),
            spreadsheetId = spreadsheetIdFixture,
            sheetId = sheetIdFixture,
            sheetName = sheetNameFixture,
            lastUpdatedAt = null
        )
    }
//
//    @Test
//    fun `` () {
//    }
}