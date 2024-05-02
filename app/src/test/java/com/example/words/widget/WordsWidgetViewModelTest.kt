package com.example.words.widget

import com.example.words.logging.Logger
import com.example.words.model.Widget
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import com.example.words.repository.WordsSynchronizer
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
import java.util.UUID
import kotlin.random.Random
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
        every { mockWidgetSettingsRepository.observeSettings(widgetFixture.id) } returns flowOf(widgetFixture)
        every { mockWordsRepository.observeRandomWords(widgetFixture.id) } returns flowOf(wordsFixture)
    }

    @Test
    fun `given the widget has been updated_when view model is widget details state should contain correct state with filled last update date`() =
        runTest(UnconfinedTestDispatcher()) {
            every { mockWidgetSettingsRepository.observeSettings(widgetId = widgetFixture.id) } returns flowOf(
                widgetFixture.copy(lastUpdatedAt = Instant.parse("2024-04-21T19:00:00.00Z"))
            )
            viewModel = createViewModel()

            assertEquals(
                expected = WidgetDetailsState(sheetName = widgetFixture.sheetName, lastUpdatedAt = "Apr 21, 2024, 7:00 PM"),
                actual = viewModel.widgetDetailsState.first()
            )
            verifyAll {
                mockWidgetSettingsRepository.observeSettings(widgetFixture.id)
            }
        }

    @Test
    fun `given the widget has not been updated_when view model is widget details state should contain successful state with empty last update date`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = createViewModel()

            assertEquals(expected = WidgetDetailsState(sheetName = widgetFixture.sheetName, lastUpdatedAt = ""), actual = viewModel.widgetDetailsState.first())
            verifyAll {
                mockWidgetSettingsRepository.observeSettings(widgetFixture.id)
            }
        }

    @Test
    fun `given there are words_when view model is created_words state should contain successful state`() = runTest(UnconfinedTestDispatcher()) {
        viewModel = createViewModel()

        val states = collectWordsStates()

        assertEquals(expected = WidgetState.Success(wordsFixture), actual = states.last())
        verifyAll {
            mockWidgetSettingsRepository.observeSettings(widgetFixture.id)
            mockWordsRepository.observeRandomWords(widgetFixture.id)
        }
    }

    @Test
    fun `given observing words fails_when view model is created_words state should contain failed state`() = runTest(UnconfinedTestDispatcher()) {
        every { mockWordsRepository.observeRandomWords(widgetFixture.id) } throws Exception("Boom!")
        every { mockLogger.e(any(), any(), any()) } just runs
        viewModel = createViewModel()

        val states = collectWordsStates()

        assertEquals(expected = WidgetState.Failure, actual = states.last())
        verifyAll {
            mockWidgetSettingsRepository.observeSettings(widgetFixture.id)
            mockWordsRepository.observeRandomWords(widgetFixture.id)
        }
    }

    @Test
    fun `given there are no words_when view model is created_words state should contain failed state`() = runTest(UnconfinedTestDispatcher()) {
        every { mockWordsRepository.observeRandomWords(widgetFixture.id) } returns flowOf(null)
        viewModel = createViewModel()

        val states = collectWordsStates()

        assertEquals(expected = WidgetState.Failure, actual = states.last())
        verifyAll {
            mockWidgetSettingsRepository.observeSettings(widgetFixture.id)
            mockWordsRepository.observeRandomWords(widgetFixture.id)
        }
    }

    @Test
    fun `when reshuffling words_words state should contain new set of words`() = runTest(UnconfinedTestDispatcher()) {
        var firstRun = true
        every { mockWordsRepository.observeRandomWords(widgetFixture.id) } answers {
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
            mockWidgetSettingsRepository.observeSettings(widgetFixture.id)
            mockWordsRepository.observeRandomWords(widgetFixture.id)
        }
        verify(exactly = 2) { mockWordsRepository.observeRandomWords(widgetFixture.id) }
    }

    @Test
    fun `when synchronizing words_words state should be in progress`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { mockWordsSynchronizer.synchronizeWords(widgetFixture.id) } just runs
        viewModel = createViewModel()

        val states = collectWordsStates()
        viewModel.synchronizeWords()

        assertEquals(expected = WidgetState.InProgress, actual = states.last())
        coVerify { mockWordsSynchronizer.synchronizeWords(widgetFixture.id) }
    }

    @Test
    fun `when widget is deleted_its settings should be deleted`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { mockWidgetSettingsRepository.deleteWidget(widgetFixture.id) } just runs
        viewModel = createViewModel()

        viewModel.deleteWidget()

        coVerify { mockWidgetSettingsRepository.deleteWidget(widgetFixture.id) }
    }

    private fun createViewModel() = WordsWidgetViewModel(
        widgetId = widgetFixture.id,
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
        val wordsFixture = listOf(UUID.randomUUID().toString() to UUID.randomUUID().toString())
        val otherWordsFixture = listOf(UUID.randomUUID().toString() to UUID.randomUUID().toString())
        val widgetFixture = Widget(
            id = getRandomWidgetId(),
            spreadsheetId = UUID.randomUUID().toString(),
            sheetId = Random.nextInt(),
            sheetName = UUID.randomUUID().toString(),
            lastUpdatedAt = null
        )
        fun getRandomWidgetId() = Widget.WidgetId(Random.nextInt())
    }
}