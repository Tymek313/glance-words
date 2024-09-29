package com.example.words.widget

import com.example.words.coroutines.collectToListInBackground
import com.example.words.logging.Logger
import com.example.words.randomSheet
import com.example.words.randomWidget
import com.example.words.randomWordPair
import com.example.words.repository.WidgetLoadingStateNotifier
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WordsWidgetViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var mockWidgetRepository: WidgetRepository
    private lateinit var mockWidgetLoadingStateNotifier: WidgetLoadingStateNotifier
    private lateinit var mockWordsRepository: WordsRepository
    private lateinit var mockLogger: Logger

    @Before
    fun setUp() {
        mockWidgetRepository = mockk()
        mockWidgetLoadingStateNotifier = mockk()
        mockWordsRepository = mockk()
        mockLogger = mockk()
        every { mockWidgetRepository.observeWidget(widgetFixture.id) } returns flowOf(widgetFixture)
        every { mockWordsRepository.observeWords(widgetFixture.id) } returns flowOf(wordsFixture)
        every { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns flowOf(false)
    }

    @Test
    fun `when widget settings are received_given widget has ever been updated_then correct widget details state is emitted`() = runTest(dispatcher) {
        every { mockWidgetRepository.observeWidget(widgetId = widgetFixture.id) } returns flowOf(
            widgetFixture.copy(sheet = widgetFixture.sheet.copy(lastUpdatedAt = Instant.parse("2024-04-21T19:00:00.00Z")))
        )
        val viewModel = createViewModel()

        assertEquals(
            WidgetDetailsState(
                sheetName = widgetFixture.sheet.name,
                lastUpdatedAt = "Apr 21, 2024, 7:00 PM"
            ),
            viewModel.widgetDetailsState.single()
        )
    }

    @Test
    fun `when widget settings are received_given widget has never been updated_then correct widget details state containing empty last updated date is emitted`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        assertEquals(
            WidgetDetailsState(
                sheetName = widgetFixture.sheet.name,
                lastUpdatedAt = ""
            ),
            viewModel.widgetDetailsState.single()
        )
    }

    @Test
    fun `when words are received_given words are emitted_then words state contains shuffled words`() = runTest(dispatcher) {
        val states = collectToListInBackground(createViewModel().wordsState)

        assertShuffled(states.single())
    }

    @Test
    fun `when words are received_given words are emitted_then words state contains at most 50 elements`() = runTest(dispatcher) {
        val randomLowerCount = Random.nextInt(48)
        every { mockWordsRepository.observeWords(widgetFixture.id) } returns flowOf(
            getRandomWords(Random.nextInt(52, 100)),
            getRandomWords(51),
            getRandomWords(50),
            getRandomWords(49),
            getRandomWords(randomLowerCount)
        )

        val states = collectToListInBackground(createViewModel().wordsState)

        assertEquals(50, assertIs<WidgetWordsState.Success>(states[0]).words.size)
        assertEquals(50, assertIs<WidgetWordsState.Success>(states[1]).words.size)
        assertEquals(50, assertIs<WidgetWordsState.Success>(states[2]).words.size)
        assertEquals(49, assertIs<WidgetWordsState.Success>(states[3]).words.size)
        assertEquals(randomLowerCount, assertIs<WidgetWordsState.Success>(states[4]).words.size)
    }

    @Test
    fun `when words are received_given exception is thrown_then words state indicates failure`() = runTest(dispatcher) {
        every { mockWordsRepository.observeWords(widgetFixture.id) } returns flow { throw Exception() }
        every { mockLogger.e(any(), any(), any()) } just runs
        val states = collectToListInBackground(createViewModel().wordsState)

        assertEquals(WidgetWordsState.Failure, states.single())
    }

    @Test
    fun `when loading state is received_given widget is reported as loading_then words state indicates loading`() = runTest(dispatcher) {
        val isLoadingFlow = MutableSharedFlow<Boolean>()
        every { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns isLoadingFlow
        val states = collectToListInBackground(createViewModel().wordsState)

        isLoadingFlow.emit(true)

        assertIs<WidgetWordsState.Loading>(states.last())
    }

    @Test
    fun `when loading state is received_given widget is reported as not loading and words are emitted_then words state indicates success`() = runTest(dispatcher) {
        val isLoadingFlow = MutableSharedFlow<Boolean>()
        every { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns isLoadingFlow
        val states = collectToListInBackground(createViewModel().wordsState)
        isLoadingFlow.emit(true)
        advanceTimeBy(1.seconds)

        isLoadingFlow.emit(false)

        val state = states.last()
        assertIs<WidgetWordsState.Success>(state)
    }

    @Test
    fun `when reshuffling words_then words state contains reshuffled words`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val states = collectToListInBackground(viewModel.wordsState)

        viewModel.reshuffleWords()

        assertShuffled(states.last())
    }

    @Test
    fun `when widget is deleted_then its settings are deleted from repository`() = runTest(dispatcher) {
        coEvery { mockWidgetRepository.deleteWidget(widgetFixture.id) } just runs
        val viewModel = createViewModel()

        viewModel.deleteWidget()

        coVerify { mockWidgetRepository.deleteWidget(widgetFixture.id) }
    }

    private fun createViewModel() = WordsWidgetViewModel(
        widgetId = widgetFixture.id,
        widgetRepository = mockWidgetRepository,
        widgetLoadingStateNotifier = mockWidgetLoadingStateNotifier,
        wordsRepository = mockWordsRepository,
        locale = Locale.US,
        zoneId = ZoneId.of("UTC"),
        logger = mockLogger
    )

    private fun assertShuffled(lastState: WidgetWordsState) {
        assertIs<WidgetWordsState.Success>(lastState)
        assertNotEquals(wordsFixture, lastState.words)
        assertEquals(wordsFixture.size, lastState.words.size)
        assertTrue(lastState.words.containsAll(wordsFixture))
    }

    private companion object {
        fun getRandomWords(size: Int) = List(size) { randomWordPair() }
        val wordsFixture = getRandomWords(10)
        val widgetFixture = randomWidget().copy(sheet = randomSheet().copy(lastUpdatedAt = null))
    }
}