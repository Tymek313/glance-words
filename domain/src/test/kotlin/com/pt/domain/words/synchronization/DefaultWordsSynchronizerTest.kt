package com.pt.domain.words.synchronization

import com.pt.domain.words.fixture.existingSheetFixture
import com.pt.domain.words.fixture.instantFixture
import com.pt.domain.words.fixture.widgetIdFixture
import com.pt.domain.words.fixture.widgetWithExistingSheetFixture
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.glancewords.domain.synchronization.DefaultWordsSynchronizer
import com.pt.glancewords.domain.synchronization.WordsSynchronizationStateNotifier
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DefaultWordsSynchronizerTest {

    private lateinit var synchronizer: DefaultWordsSynchronizer
    private lateinit var fakeWordsRepository: WordsRepository
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeSheetRepository: SheetRepository
    private lateinit var fakeGetNowInstant: () -> Instant
    private lateinit var fakeWordsSynchronizationStateNotifier: WordsSynchronizationStateNotifier
    private lateinit var fakeRefreshWidget: suspend (WidgetId) -> Unit

    private val everyObserveWidget get() = coEvery { fakeWidgetRepository.observeWidget(widgetIdFixture) }
    private val everySynchronizeWords get() = coEvery { fakeWordsRepository.synchronizeWords(any()) }
    private val everyUpdateLastUpdatedAt get() = coEvery { fakeSheetRepository.updateLastUpdatedAt(existingSheetFixture.id, instantFixture) }
    private val everyNotifyWordsSynchronizationForAction get() = coEvery {
        fakeWordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction(widgetIdFixture, captureLambda())
    }
    private val everyGetNowInstant get() = every { fakeGetNowInstant() }
    private val everyRefreshWidget get() = coEvery { fakeRefreshWidget(widgetIdFixture) }
    private val everyDeleteCachedWords get() = coEvery { fakeWordsRepository.deleteCachedWords(widgetIdFixture) }

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
            refreshWidget = fakeRefreshWidget
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `when words are synchronized_given widget do not exist_then exception is thrown`() = runTest {
        cachedWordsAreDeleted()
        noWidgetIsEmitted()

        synchronizer.synchronizeWords(widgetIdFixture)
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

        synchronizer.synchronizeWords(widgetIdFixture)

        coVerifySequence {
            fakeWordsRepository.deleteCachedWords(widgetIdFixture)
            fakeWidgetRepository.observeWidget(widgetIdFixture)
            fakeRefreshWidget(widgetIdFixture)
            fakeWordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction(widgetIdFixture, any())
            fakeWordsRepository.synchronizeWords(any())
            fakeSheetRepository.updateLastUpdatedAt(existingSheetFixture.id, instantFixture)
        }
    }

    @Test
    fun `when words are synchronized_given widget exists_then widget is refreshed`() = runTest(UnconfinedTestDispatcher()) {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshedSuspended()

        backgroundScope.launch {
            synchronizer.synchronizeWords(widgetIdFixture)
        }

        coVerify { fakeRefreshWidget(widgetIdFixture) }
    }

    @Test
    fun `when words are synchronized_given widget exists_then widget loading state is triggered`() = runTest(UnconfinedTestDispatcher()) {
        cachedWordsAreDeleted()
        widgetIsEmitted()
        widgetIsRefreshed()
        notifyWordsSynchronizationSuspended()

        backgroundScope.launch {
            synchronizer.synchronizeWords(widgetIdFixture)
        }

        coVerify { fakeWordsSynchronizationStateNotifier.notifyWordsSynchronizationForAction(widgetIdFixture, any()) }
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

        synchronizer.synchronizeWords(widgetIdFixture)

        coVerify {
            fakeWordsRepository.synchronizeWords(
                widgetWithExistingSheetFixture.run { WordsRepository.SynchronizationRequest(id, sheet.sheetSpreadsheetId) }
            )
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

        synchronizer.synchronizeWords(widgetIdFixture)

        coVerify { fakeSheetRepository.updateLastUpdatedAt(widgetWithExistingSheetFixture.sheet.id, instantFixture) }
    }

    private fun widgetIsEmitted() = everyObserveWidget returns flowOf(widgetWithExistingSheetFixture)

    private fun noWidgetIsEmitted() = everyObserveWidget returns flowOf(null)

    private fun wordsAreSynchronized() = everySynchronizeWords just runs

    private fun lastUpdatedPropertyIsUpdated() = everyUpdateLastUpdatedAt just runs

    private fun notifyWordsSynchronization() = everyNotifyWordsSynchronizationForAction coAnswers { lambda<suspend () -> Unit>().coInvoke() }

    private fun notifyWordsSynchronizationSuspended() = everyNotifyWordsSynchronizationForAction just awaits

    private fun cachedWordsAreDeleted() = everyDeleteCachedWords just runs

    private fun widgetIsRefreshed() = everyRefreshWidget just runs

    private fun widgetIsRefreshedSuspended() = everyRefreshWidget just awaits

    private fun instantIsReturned() = everyGetNowInstant returns instantFixture

}