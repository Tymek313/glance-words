package com.example.words.widget

import com.example.words.coroutines.collectToListInBackground
import com.example.words.domain.WidgetLoadingStateNotifier
import com.example.words.logging.Logger
import com.example.words.randomWidgetWithNewSheet
import com.example.words.randomWordPair
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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WordsWidgetViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeWidgetLoadingStateNotifier: WidgetLoadingStateNotifier
    private lateinit var fakeWordsRepository: WordsRepository
    private lateinit var fakeLogger: Logger
    private lateinit var fakeReshuffleNotifier: ReshuffleNotifier

    @Before
    fun setUp() {
        fakeWidgetRepository = mockk()
        fakeWidgetLoadingStateNotifier = mockk()
        fakeWordsRepository = mockk()
        fakeLogger = mockk()
        fakeReshuffleNotifier = mockk()
        every { fakeWidgetRepository.observeWidget(widgetFixture.id) } returns flowOf(widgetFixture)
        every { fakeWordsRepository.observeWords(widgetFixture.id) } returns flowOf(emptyList())
        every { fakeWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns flowOf(false)
        every { fakeReshuffleNotifier.shouldReshuffle } returns flowOf(false)
    }

    @Test
    fun `when widget settings are received_given widget has ever been updated_then correct state is emitted`() = runTest(dispatcher) {
        every { fakeWidgetRepository.observeWidget(widgetId = widgetFixture.id) } returns flowOf(
            widgetFixture.copy(sheet = widgetFixture.sheet.copy(lastUpdatedAt = Instant.parse("2024-04-21T19:00:00.00Z")))
        )
        val viewModel = createViewModel()

        assertEquals(
            WidgetUiState(
                sheetName = widgetFixture.sheet.name,
                lastUpdatedAt = "Apr 21, 2024, 7:00 PM",
            ),
            viewModel.uiState.single()
        )
    }

    @Test
    fun `when widget settings are received_given widget has never been updated_then correct state containing empty last updated date is emitted`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        assertEquals(
            WidgetUiState(
                sheetName = widgetFixture.sheet.name,
                lastUpdatedAt = ""
            ),
            viewModel.uiState.single()
        )
    }

    @Test
    fun `when words are received_then state contains shuffled words`() = runTest(dispatcher) {
        every { fakeWordsRepository.observeWords(widgetFixture.id) } returns flowOf(wordsFixture)
        val states = collectToListInBackground(createViewModel().uiState)

        assertEquals(wordsFixture.size, states.single().words.size)
        assertNotEquals(wordsFixture, states.single().words)
    }

    @Test
    fun `when words are received_then state contains at most 50 elements`() = runTest(dispatcher) {
        val randomLowerCount = Random.nextInt(48)
        every { fakeWordsRepository.observeWords(widgetFixture.id) } returns flowOf(
            getRandomWords(Random.nextInt(52, 100)),
            getRandomWords(51),
            getRandomWords(50),
            getRandomWords(49),
            getRandomWords(randomLowerCount)
        )

        val states = collectToListInBackground(createViewModel().uiState)

        assertEquals(50, states[0].words.size)
        assertEquals(50, states[1].words.size)
        assertEquals(50, states[2].words.size)
        assertEquals(49, states[3].words.size)
        assertEquals(randomLowerCount, states[4].words.size)
    }

    @Test
    fun `when loading state is received_given widget is reported as loading_then state indicates loading`() = runTest(dispatcher) {
        val isLoadingFlow = MutableSharedFlow<Boolean>()
        every { fakeWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns isLoadingFlow
        val states = collectToListInBackground(createViewModel().uiState)

        isLoadingFlow.emit(true)

        assertTrue(states.last().isLoading)
    }

    @Test
    fun `when loading state is received_given widget is reported as not loading_then state does not indicate loading`() = runTest(dispatcher) {
        val isLoadingFlow = MutableSharedFlow<Boolean>()
        every { fakeWidgetLoadingStateNotifier.observeIsWidgetLoading(widgetFixture.id) } returns isLoadingFlow
        val states = collectToListInBackground(createViewModel().uiState)
        isLoadingFlow.emit(true)
        advanceTimeBy(1.seconds)

        isLoadingFlow.emit(false)

        assertFalse(states.last().isLoading)
    }

    @Test
    fun `when reshuffling event is received_then state contains new set of words`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val reshufflingFlow = MutableSharedFlow<Boolean>()
        every { fakeWordsRepository.observeWords(widgetFixture.id) } returns flowOf(getRandomWords(10)) andThen flowOf(wordsFixture)
        coEvery { fakeReshuffleNotifier.shouldReshuffle } returns reshufflingFlow
        val states = collectToListInBackground(viewModel.uiState)

        reshufflingFlow.emit(true)

        assertEquals(states[0].words.size, states[1].words.size)
        assertNotEquals(states[0].words, states[1].words)
    }

    @Test
    fun `when widget is deleted_then its settings are deleted from repository`() = runTest(dispatcher) {
        coEvery { fakeWidgetRepository.deleteWidget(widgetFixture.id) } just runs
        val viewModel = createViewModel()

        viewModel.deleteWidget()

        coVerify { fakeWidgetRepository.deleteWidget(widgetFixture.id) }
    }

    private fun createViewModel() = WordsWidgetViewModel(
        widgetId = widgetFixture.id,
        widgetRepository = fakeWidgetRepository,
        widgetLoadingStateNotifier = fakeWidgetLoadingStateNotifier,
        wordsRepository = fakeWordsRepository,
        locale = Locale.US,
        zoneId = ZoneId.of("UTC"),
        logger = fakeLogger,
        reshuffleNotifier = fakeReshuffleNotifier
    )

    private companion object {
        fun getRandomWords(size: Int) = List(size) { randomWordPair() }
        val wordsFixture = getRandomWords(10)
        val widgetFixture = randomWidgetWithNewSheet().run { copy(sheet = sheet.copy(lastUpdatedAt = null)) }
    }
}