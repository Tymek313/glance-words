package com.example.words.repository

import com.example.words.model.Widget
import com.example.words.randomWidget
import com.example.words.randomWidgetId
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertTrue

class DefaultWordsSynchronizerTest {

    private lateinit var synchronizer: DefaultWordsSynchronizer
    private lateinit var fakeWordsRepository: WordsRepository
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeSheetRepository: SheetRepository
    private lateinit var fakeGetNowInstant: () -> Instant
    private lateinit var fakeWidgetLoadingStateNotifier: WidgetLoadingStateNotifier
    private lateinit var fakeUpdateWidget: (Widget.WidgetId) -> Unit

    @Before
    fun setUp() {
        fakeWordsRepository = mockk()
        fakeWidgetRepository = mockk()
        fakeGetNowInstant = mockk()
        fakeWidgetLoadingStateNotifier = mockk()
        fakeUpdateWidget = mockk()
        fakeSheetRepository = mockk()
        synchronizer = DefaultWordsSynchronizer(
            wordsRepository =  fakeWordsRepository,
            widgetRepository = fakeWidgetRepository,
            sheetRepository = fakeSheetRepository,
            getNowInstant = fakeGetNowInstant,
            widgetLoadingStateNotifier = fakeWidgetLoadingStateNotifier,
            refreshWidget = fakeUpdateWidget
        )
        coEvery { fakeWidgetRepository.observeWidget(any()) } returns flowOf(randomWidget())
        coEvery { fakeWordsRepository.synchronizeWords(any()) } just runs
        coEvery { fakeSheetRepository.updateLastUpdatedAt(any(), any()) } just runs
        coEvery { fakeWidgetLoadingStateNotifier.setLoadingWidgetForAction(any(), captureLambda()) } coAnswers {
            lambda<suspend () -> Unit>().coInvoke()
        }
        every { fakeGetNowInstant() } returns Instant.now()
        every { fakeUpdateWidget(any()) } just runs
        coEvery { fakeWordsRepository.deleteCachedWords(any()) } just runs
    }

    @Test
    fun `when words are synchronized_given widget settings do not exist_then exception is thrown`() = runTest {
        val widget = randomWidget()
        coEvery { fakeWidgetRepository.observeWidget(widget.id) } returns flowOf(null)
        var exceptionThrown = false

        try {
            synchronizer.synchronizeWords(widget.id)
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then all steps are executed in the correct order`() = runTest {
        synchronizer.synchronizeWords(randomWidgetId())

        coVerifySequence {
            fakeWidgetRepository.observeWidget(any())
            fakeUpdateWidget(any())
            fakeWidgetLoadingStateNotifier.setLoadingWidgetForAction(any(), any())
            fakeWordsRepository.synchronizeWords(any())
            fakeSheetRepository.updateLastUpdatedAt(any(), any())
        }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then widget is updated`() = runTest {
        val widgetId = randomWidgetId()

        synchronizer.synchronizeWords(widgetId)

        coVerify { fakeUpdateWidget(widgetId) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then widget loading state is triggered`() = runTest {
        val widgetId = randomWidgetId()

        synchronizer.synchronizeWords(widgetId)

        coVerify { fakeWidgetLoadingStateNotifier.setLoadingWidgetForAction(widgetId, any()) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then words are synchronized in the repository`() = runTest {
        val widget = randomWidget()
        val widgetId = widget.id
        val expectedSyncRequest = widget.run { WordsRepository.SynchronizationRequest(id, widget.sheet.sheetSpreadsheetId) }
        coEvery { fakeWidgetRepository.observeWidget(widgetId) } returns flowOf(widget)

        synchronizer.synchronizeWords(widgetId)

        coVerify { fakeWordsRepository.synchronizeWords(expectedSyncRequest) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then last update date of the correct widget is updated`() = runTest {
        val widget = randomWidget()
        val currentTime = Instant.now()
        every { fakeGetNowInstant.invoke() } returns currentTime
        coEvery { fakeWidgetRepository.observeWidget(any()) } returns flowOf(widget)

        synchronizer.synchronizeWords(widget.id)

        coVerify { fakeSheetRepository.updateLastUpdatedAt(widget.sheet.id, currentTime) }
    }
}