package com.pt.glancewords.widget

import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.model.WordPair
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.glancewords.domain.synchronization.WordsSynchronizationStateNotifier
import com.pt.glancewords.logging.Logger
import com.pt.testcommon.coroutines.collectToListInBackground
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

    private val everyObserveWidget get() = every { fakeWidgetRepository.observeWidget(WIDGET.id) }
    private val everyObserveWords get() = every { fakeWordsRepository.observeWords(WIDGET.id) }
    private val everyObserveAreWordsSynchronized get() = every { fakeWordsSynchronizationStateNotifier.observeAreWordsSynchronized(WIDGET.id) }
    private val everyReshuffleEvents get() = every { fakeReshuffleNotifier.reshuffleEvents }

    private fun widgetIsEmitted(widget: Widget = WIDGET) = everyObserveWidget returns flowOf(widget)
    private fun noWordsAreEmitted() = everyObserveWords returns flowOf(emptyList())
    private fun wordsAreEmitted() = everyObserveWords returns flowOf(WORDS)
    private fun multipleWordListsAreEmitted(first: List<WordPair>, second: List<WordPair>) = everyObserveWords returns flowOf(first) andThen flowOf(second)
    private fun wordsAreNotLoading() = everyObserveAreWordsSynchronized returns flowOf(false)
    private fun wordsAreLoadingDeferred() = MutableStateFlow(false).also { everyObserveAreWordsSynchronized returns it }
    private fun shouldReshuffleIsDeferred() = Channel<Unit>(Channel.CONFLATED).also { everyReshuffleEvents returns it }
    private fun widgetIsDeleted() = coEvery { fakeWidgetRepository.deleteWidget(WIDGET.id) } just runs

    @Before
    fun setUp() {
        fakeWidgetRepository = mockk()
        fakeWordsSynchronizationStateNotifier = mockk()
        fakeWordsRepository = mockk()
        fakeLogger = mockk()
        fakeReshuffleNotifier = mockk()
    }

    @Test
    fun `when widget is received_given widget has ever been updated_then correct state is emitted`() = runTest(dispatcher) {
        widgetIsEmitted(WIDGET.withLastUpdatedAt("2024-04-21T19:00:00.00Z"))
        noWordsAreEmitted()
        wordsAreNotLoading()
        shouldReshuffleIsDeferred()

        val states = collectToListInBackground(createViewModel().uiState)

        assertEquals(
            WidgetUiState(
                sheetName = WIDGET.sheet.name,
                lastUpdatedAt = "Apr 21, 2024, 7:00 PM",
            ),
            states.single()
        )
    }

    @Test
    fun `when widget is received_given widget has never been updated_then correct state containing empty last updated date is emitted`() = runTest(dispatcher) {
        widgetIsEmitted()
        noWordsAreEmitted()
        wordsAreNotLoading()
        shouldReshuffleIsDeferred()

        val states = collectToListInBackground(createViewModel().uiState)

        assertEquals(
            WidgetUiState(
                sheetName = WIDGET.sheet.name,
                lastUpdatedAt = ""
            ),
            states.single()
        )
    }

    @Test
    fun `when words are received_then state contains shuffled words`() = runTest(dispatcher) {
        widgetIsEmitted()
        wordsAreEmitted()
        wordsAreNotLoading()
        shouldReshuffleIsDeferred()

        val states = collectToListInBackground(createViewModel().uiState)

        assertEquals(WORDS.size, states.single().words.size)
        assertNotEquals(WORDS, states.single().words)
    }

    @Test
    fun `when words are received_then state contains at most 50 elements`() = runTest(dispatcher) {
        widgetIsEmitted()
        wordsAreEmitted()
        wordsAreNotLoading()
        shouldReshuffleIsDeferred()
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
        shouldReshuffleIsDeferred()
        wordsAreEmitted()
        val isLoadingFlow = wordsAreLoadingDeferred()
        val states = collectToListInBackground(createViewModel().uiState)

        isLoadingFlow.emit(true)

        assertTrue(states.last().isLoading)
    }

    @Test
    fun `when loading state is received_given widget is reported as not loading_then state does not indicate loading`() = runTest(dispatcher) {
        shouldReshuffleIsDeferred()
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
        multipleWordListsAreEmitted(getRandomWords(), WORDS)
        val reshuffleFlow = shouldReshuffleIsDeferred()
        val states = collectToListInBackground(createViewModel().uiState)

        reshuffleFlow.send(Unit)

        assertEquals(WORDS.size, states.last().words.size)
        assertTrue(states.last().words.containsAll(WORDS))
    }

    @Test
    fun `when widget is deleted_then its settings are deleted from repository`() = runTest(dispatcher) {
        widgetIsEmitted()
        wordsAreNotLoading()
        wordsAreEmitted()
        shouldReshuffleIsDeferred()
        widgetIsDeleted()
        val viewModel = createViewModel()

        viewModel.deleteWidget()

        coVerify { fakeWidgetRepository.deleteWidget(WIDGET.id) }
    }

    private fun Widget.withLastUpdatedAt(isoLastUpdatedAt: String) = copy(sheet = sheet.copy(lastUpdatedAt = Instant.parse(isoLastUpdatedAt)))

    private fun createViewModel() = WordsWidgetViewModel(
        widgetId = WIDGET.id,
        widgetRepository = fakeWidgetRepository,
        wordsSynchronizationStateNotifier = fakeWordsSynchronizationStateNotifier,
        wordsRepository = fakeWordsRepository,
        locale = Locale.US,
        zoneId = ZoneId.of("UTC"),
        logger = fakeLogger,
        reshuffleNotifier = fakeReshuffleNotifier
    )

    private companion object {
        fun getRandomWords(size: Int = 10) = List(size) { WordPair(randomString(), randomString()) }
        val WORDS = getRandomWords()
        val WIDGET = Widget(
            id = WidgetId(randomInt()),
            sheet = Sheet(
                id = SheetId(randomInt()),
                sheetSpreadsheetId = SheetSpreadsheetId(spreadsheetId = randomString(), sheetId = randomInt()),
                name = randomString(),
                lastUpdatedAt = null
            )
        )
    }
}