package com.pt.glancewords.domain.synchronization

import com.pt.glancewords.domain.fixture.randomSheetId
import com.pt.glancewords.domain.fixture.randomWidgetId
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.testcommon.fixture.randomInstant
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultWordsSynchronizerTest {

    private lateinit var synchronizer: DefaultWordsSynchronizer
    private lateinit var fakeWordsRepository: WordsRepository
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeSheetRepository: SheetRepository
    private lateinit var fakeGetNowInstant: () -> Instant
    private lateinit var fakeWordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier
    private lateinit var fakeRefreshWidget: suspend (WidgetId) -> Unit

    private val everyGetWidget get() = coEvery { fakeWidgetRepository.getWidget(WIDGET_ID_TO_SYNCHRONIZE) }
    private val everySynchronizeWords get() = coEvery { fakeWordsRepository.synchronizeWords(STORED_WIDGET.sheet.id, STORED_SHEET.sheetSpreadsheetId) }
    private val everyUpdateLastUpdatedAt get() = coEvery { fakeSheetRepository.updateLastUpdatedAt(STORED_SHEET.id, NOW) }
    private val everyNotifyWordsSynchronizationForAction get() = coEvery {
        fakeWordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction<Any>(STORED_WIDGET.id, captureLambda())
    }
    private val everyGetNowInstant get() = every { fakeGetNowInstant() }
    private val everyRefreshWidget get() = coEvery { fakeRefreshWidget(STORED_WIDGET.id) }
    private val everyDeleteCachedWords get() = coEvery { fakeWordsRepository.deleteWords(STORED_WIDGET.sheet.id) }

    @Before
    fun setUp() {
        fakeWordsRepository = mockk()
        fakeWidgetRepository = mockk()
        fakeGetNowInstant = mockk()
        fakeWordsSynchronizationStateNotifier = mockk()
        fakeRefreshWidget = mockk()
        fakeSheetRepository = mockk()
        synchronizer = DefaultWordsSynchronizer(
            wordsRepository = fakeWordsRepository,
            widgetRepository = fakeWidgetRepository,
            sheetRepository = fakeSheetRepository,
            getNowInstant = fakeGetNowInstant,
            wordsSynchronizationStateNotifier = fakeWordsSynchronizationStateNotifier,
            refreshWidget = fakeRefreshWidget,
            logger = mockk(relaxed = true)
        )
    }

    @Test
    fun `when words are synchronized_given widget do not exist_then false is returned`() = runTest {
        cachedWordsAreDeleted()
        noWidgetIsEmitted()

        val syncSucceeded = synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)

        assertFalse(syncSucceeded)
    }

    @Test
    fun `when words are synchronized_given widget exists_then all steps are executed in the correct order`() = runTest {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshed()
        notifyWordsSynchronization()
        wordsAreSynchronized()
        lastUpdatedPropertyIsUpdated()
        instantIsReturned()

        synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)

        coVerifySequence {
            fakeWidgetRepository.getWidget(any())
            fakeWordsRepository.deleteWords(any())
            fakeRefreshWidget(any())
            fakeWordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction<Any>(any(), any())
            fakeWordsRepository.synchronizeWords(any(), any())
            fakeSheetRepository.updateLastUpdatedAt(any(), any())
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `when words are synchronized_given widget exists_then widget is refreshed`() = runTest(UnconfinedTestDispatcher()) {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshedSuspended()

        backgroundScope.launch {
            synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)
        }

        coVerify { fakeRefreshWidget(STORED_WIDGET.id) }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `when words are synchronized_given widget exists_then widget loading state is triggered`() = runTest(UnconfinedTestDispatcher()) {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshed()
        notifyWordsSynchronizationSuspended()

        backgroundScope.launch {
            synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)
        }

        coVerify { fakeWordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction(STORED_WIDGET.id, any()) }
    }

    @Test
    fun `when words are synchronized_given widget exists_then words are synchronized in the repository`() = runTest {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshed()
        notifyWordsSynchronization()
        lastUpdatedPropertyIsUpdated()
        wordsAreSynchronized()
        instantIsReturned()

        synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)

        coVerify {
            fakeWordsRepository.synchronizeWords(STORED_WIDGET.sheet.id, STORED_WIDGET.sheet.sheetSpreadsheetId)
        }
    }

    @Test
    fun `when words are synchronized_given widget exists_then last update date of the correct widget is updated`() = runTest {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshed()
        notifyWordsSynchronization()
        wordsAreSynchronized()
        lastUpdatedPropertyIsUpdated()
        instantIsReturned()

        synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)

        coVerify { fakeSheetRepository.updateLastUpdatedAt(STORED_WIDGET.sheet.id, NOW) }
    }

    @Test
    fun `when words are synchronized_given synchronization fails_then false is returned`() = runTest {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshed()
        notifyWordsSynchronization()
        wordsSynchronizationFails()

        val syncSucceeded = synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)

        assertFalse(syncSucceeded)
    }

    @Test
    fun `when words are synchronized_given synchronization succeeds_then true is returned`() = runTest {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshed()
        notifyWordsSynchronization()
        wordsAreSynchronized()
        lastUpdatedPropertyIsUpdated()
        instantIsReturned()

        val syncSucceeded = synchronizer.synchronizeWords(WIDGET_ID_TO_SYNCHRONIZE)

        assertTrue(syncSucceeded)
    }

    private fun widgetIsEmitted() = everyGetWidget returns STORED_WIDGET

    private fun noWidgetIsEmitted() = everyGetWidget returns null

    private fun wordsAreSynchronized() = everySynchronizeWords returns true

    private fun wordsSynchronizationFails() = everySynchronizeWords returns false

    private fun lastUpdatedPropertyIsUpdated() = everyUpdateLastUpdatedAt just runs

    private fun notifyWordsSynchronization() = everyNotifyWordsSynchronizationForAction coAnswers { lambda<suspend () -> Boolean>().coInvoke() }

    private fun notifyWordsSynchronizationSuspended() = everyNotifyWordsSynchronizationForAction just awaits

    private fun cachedWordsAreDeleted() = everyDeleteCachedWords just runs

    private fun widgetIsRefreshed() = everyRefreshWidget just runs

    private fun widgetIsRefreshedSuspended() = everyRefreshWidget just awaits

    private fun instantIsReturned() = everyGetNowInstant returns NOW

    private companion object {
        val NOW = randomInstant()
        val WIDGET_ID_TO_SYNCHRONIZE = randomWidgetId()
        val STORED_SHEET = Sheet(
            id = randomSheetId(),
            sheetSpreadsheetId = SheetSpreadsheetId(randomString(), randomInt()),
            name = randomString(),
            lastUpdatedAt = NOW
        )
        val STORED_WIDGET = Widget(id = randomWidgetId(), sheet = STORED_SHEET)
    }
}