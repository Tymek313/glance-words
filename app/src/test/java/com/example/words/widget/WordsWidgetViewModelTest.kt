package com.example.words.widget

import com.example.words.coroutines.collectToListInBackground
import com.example.words.domain.WordsSynchronizationStateNotifier
import com.example.words.fixture.randomWidgetWithNewSheet
import com.example.words.fixture.randomWordPair
import com.example.words.logging.Logger
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.repository.WidgetRepository
import com.example.words.repository.WordsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WordsWidgetViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeWordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier
    private lateinit var fakeWordsRepository: WordsRepository
    private lateinit var fakeLogger: Logger
    private lateinit var fakeReshuffleNotifier: ReshuffleNotifier

    @Before
    fun setUp() {
        fakeWidgetRepository = mockk()
        fakeWordsSynchronizationStateNotifier = mockk()
        fakeWordsRepository = mockk()
        fakeLogger = mockk()
        fakeReshuffleNotifier = mockk()
    }

    private val everyObserveWidget get() = every { fakeWidgetRepository.observeWidget(widgetFixture.id) }
    private val everyObserveWords get() = every { fakeWordsRepository.observeWords(widgetFixture.id) }
    private val everyObserveAreWordsSynchronized get() = every { fakeWordsSynchronizationStateNotifier.observeAreWordsSynchronized(widgetFixture.id) }
    private val everyReshuffleEvents get() = every { fakeReshuffleNotifier.reshuffleEvents }

    private fun widgetIsEmitted(widget: Widget = widgetFixture) = everyObserveWidget returns flowOf(widget)
    private fun noWordsAreEmitted() = everyObserveWords returns flowOf(emptyList())
    private fun wordsAreEmitted() = everyObserveWords returns flowOf(wordsFixture)
    private fun multipleWordsAreEmitted(vararg words: List<WordPair>) = everyObserveWords returns words.asFlow()
    private fun wordsAreNotLoading() = everyObserveAreWordsSynchronized returns flowOf(false)
    private fun wordsAreLoadingDeferred() = MutableStateFlow(false).also { everyObserveAreWordsSynchronized returns it }
    private fun shouldNotReshuffle() = Channel<Unit>(Channel.CONFLATED).apply { close() }.also { everyReshuffleEvents returns it }
    private fun shouldReshuffleIsDeferred() = Channel<Unit>(Channel.CONFLATED).also { everyReshuffleEvents returns it }
    private fun widgetIsDeleted() = coEvery { fakeWidgetRepository.deleteWidget(widgetFixture.id) } just runs

    @Test
    fun `when widget is received_given widget has ever been updated_then correct state is emitted`() = runTest(dispatcher) {
        widgetIsEmitted(widgetFixture.withLastUpdatedAt("2024-04-21T19:00:00.00Z"))
        noWordsAreEmitted()
        wordsAreNotLoading()
        shouldNotReshuffle()

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
    fun `when widget is received_given widget has never been updated_then correct state containing empty last updated date is emitted`() = runTest(dispatcher) {
        widgetIsEmitted()
        noWordsAreEmitted()
        wordsAreNotLoading()
        shouldNotReshuffle()

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
        widgetIsEmitted()
        wordsAreEmitted()
        wordsAreNotLoading()
        shouldNotReshuffle()

        val states = collectToListInBackground(createViewModel().uiState)

        assertEquals(wordsFixture.size, states.single().words.size)
        assertNotEquals(wordsFixture, states.single().words)
    }

    @Test
    fun `when words are received_then state contains at most 50 elements`() = runTest(dispatcher) {
        widgetIsEmitted()
        wordsAreEmitted()
        wordsAreNotLoading()
        shouldNotReshuffle()
        val randomLowerCount = Random.nextInt(48)
        everyObserveWords returns flowOf(
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
        widgetIsEmitted()
        shouldNotReshuffle()
        wordsAreEmitted()
        val isLoadingFlow = wordsAreLoadingDeferred()
        val states = collectToListInBackground(createViewModel().uiState)

        isLoadingFlow.emit(true)

        assertTrue(states.last().isLoading)
    }

    @Test
    fun `when loading state is received_given widget is reported as not loading_then state does not indicate loading`() = runTest(dispatcher) {
        shouldNotReshuffle()
        widgetIsEmitted()
        wordsAreEmitted()
        val isLoadingFlow = wordsAreLoadingDeferred()
        val states = collectToListInBackground(createViewModel().uiState)
        isLoadingFlow.emit(true)

        isLoadingFlow.emit(false)

        assertFalse(states.last().isLoading)
    }

    @Test
    fun `when reshuffling event is received_then state contains new set of words`() = runTest(dispatcher) {
        widgetIsEmitted()
        wordsAreNotLoading()
        multipleWordsAreEmitted(getRandomWords(10), wordsFixture)
        val reshuffleFlow = shouldReshuffleIsDeferred()
        val states = collectToListInBackground(createViewModel().uiState)

        reshuffleFlow.send(Unit)

        assertEquals(wordsFixture.size, states.last().words.size)
        assertTrue(states.last().words.containsAll(wordsFixture))
    }

    @Test
    fun `when widget is deleted_then its settings are deleted from repository`() = runTest(dispatcher) {
        widgetIsEmitted()
        wordsAreNotLoading()
        wordsAreEmitted()
        shouldNotReshuffle()
        widgetIsDeleted()
        val viewModel = createViewModel()

        viewModel.deleteWidget()

        coVerify { fakeWidgetRepository.deleteWidget(widgetFixture.id) }
    }

    private fun Widget.withLastUpdatedAt(isoLastUpdatedAt: String) = copy(sheet = sheet.copy(lastUpdatedAt = Instant.parse(isoLastUpdatedAt)))

    private fun createViewModel() = WordsWidgetViewModel(
        widgetId = widgetFixture.id,
        widgetRepository = fakeWidgetRepository,
        wordsSynchronizationStateNotifier = fakeWordsSynchronizationStateNotifier,
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