package com.example.words.widget

import com.example.words.coroutines.collectToListInBackground
import com.example.words.logging.Logger
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.randomInt
import com.example.words.randomString
import com.example.words.randomWidgetId
import com.example.words.randomWordPair
import com.example.words.repository.WidgetLoadingStateNotifier
import com.example.words.repository.WidgetSettingsRepository
import com.example.words.repository.WordsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
@MockKExtension.ConfirmVerification
class WordsWidgetViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var mockWidgetSettingsRepository: WidgetSettingsRepository
    private lateinit var mockWidgetLoadingStateNotifier: WidgetLoadingStateNotifier
    private lateinit var mockWordsRepository: WordsRepository
    private lateinit var mockLogger: Logger

    @Before
    fun setUp() {
        mockWidgetSettingsRepository = mockk()
        mockWidgetLoadingStateNotifier = mockk()
        mockWordsRepository = mockk()
        mockLogger = mockk()
        every { mockWidgetSettingsRepository.observeSettings(widgetFixture.id) } returns flowOf(widgetFixture)
        every { mockWordsRepository.observeWords(widgetFixture.id) } returns flowOf(wordsFixture)
        every { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns emptyFlow()
    }

    @Test
    fun `when created_given widget has ever been updated_correct widget details state is emitted`() = runTest(dispatcher) {
        every { mockWidgetSettingsRepository.observeSettings(widgetId = widgetFixture.id) } returns flowOf(
            widgetFixture.copy(lastUpdatedAt = Instant.parse("2024-04-21T19:00:00.00Z"))
        )
        val viewModel = createViewModel()

        assertEquals(
            WidgetDetailsState(
                sheetName = widgetFixture.sheetName,
                lastUpdatedAt = "Apr 21, 2024, 7:00 PM"
            ),
            viewModel.widgetDetailsState.single()
        )
    }

    @Test
    fun `when created_given widget has never been updated_widget details state with empty last updated date is emitted`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        assertEquals(
            WidgetDetailsState(
                sheetName = widgetFixture.sheetName,
                lastUpdatedAt = ""
            ),
            viewModel.widgetDetailsState.single()
        )
    }
    
    @Test
    fun `when created_given there are words_words state contains shuffled words`() = runTest(dispatcher) {
        val states = collectToListInBackground(createViewModel().wordsState)

        assertShuffled(states.single())
    }

    @Test
    fun `when receiving words_words state contains at most 50 elements`() = runTest(dispatcher) {
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
    fun `when widget is reported as loading_given for the first time_words state is success`() = runTest(dispatcher) {
        every { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns flowOf(Unit)
        val states = collectToListInBackground(createViewModel().wordsState)

        assertIs<WidgetWordsState.Success>(states.last())
    }

    @Test
    fun `when widget is reported as loading_given for the second time_words state indicates loading`() = runTest(dispatcher) {
        coEvery { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns flowOf(Unit, Unit)
        val states = collectToListInBackground(createViewModel().wordsState)

        assertIs<WidgetWordsState.Loading>(states.last())
    }

    @Test
    fun `when widget is reported as loading_given for more than second time_words state indicates loading`() = runTest(dispatcher) {
        coEvery { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns flowOf(
            *Array(Random.nextInt(from = 3, until = 100)) {}
        )
        val states = collectToListInBackground(createViewModel().wordsState)

        assertIs<WidgetWordsState.Loading>(states.last())
    }

    @Test
    fun `when new words are emitted_given widget is loading_words state does not indicate loading`() = runTest(dispatcher) {
        val isLoadingFlow = MutableSharedFlow<Unit>()
        val wordsFlow = MutableStateFlow(emptyList<WordPair>())
        coEvery { mockWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns isLoadingFlow
        coEvery { mockWordsRepository.observeWords(widgetFixture.id) } returns wordsFlow
        val states = collectToListInBackground(createViewModel().wordsState)
        isLoadingFlow.emit(Unit)

        wordsFlow.value = listOf(randomWordPair())

        assertIs<WidgetWordsState.Success>(states.last())
    }

    @Test
    fun `when words_given observing words fails_words state contains failed state`() = runTest(dispatcher) {
        every { mockWordsRepository.observeWords(widgetFixture.id) } returns flow { throw Exception() }
        every { mockLogger.e(any(), any(), any()) } just runs
        val states = collectToListInBackground(createViewModel().wordsState)

        assertEquals(WidgetWordsState.Failure, states.single())
    }

    @Test
    fun `when reshuffling words_words state contains reshuffled words`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val states = collectToListInBackground(viewModel.wordsState)

        viewModel.reshuffleWords()

        assertShuffled(states.last())
    }

    @Test
    fun `when widget is deleted_its settings is deleted`() = runTest(dispatcher) {
        coEvery { mockWidgetSettingsRepository.deleteWidget(widgetFixture.id) } just runs
        val viewModel = createViewModel()

        viewModel.deleteWidget()

        coVerify { mockWidgetSettingsRepository.deleteWidget(widgetFixture.id) }
    }

    private fun createViewModel() = WordsWidgetViewModel(
        widgetId = widgetFixture.id,
        widgetSettingsRepository = mockWidgetSettingsRepository,
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
        val widgetFixture = Widget(
            id = randomWidgetId(),
            spreadsheetId = randomString(),
            sheetId = randomInt(),
            sheetName = randomString(),
            lastUpdatedAt = null
        )
    }
}