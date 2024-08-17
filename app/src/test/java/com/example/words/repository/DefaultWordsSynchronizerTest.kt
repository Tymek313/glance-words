package com.example.words.repository

import com.example.words.model.Widget
import com.example.words.randomWidget
import com.example.words.randomWidgetId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runners.Parameterized.Parameters
import java.time.Instant
import kotlin.test.assertTrue

@MockKExtension.ConfirmVerification
class DefaultWordsSynchronizerTest {

    private lateinit var synchronizer: DefaultWordsSynchronizer
    private lateinit var mockWordsRepository: WordsRepository
    private lateinit var mockWidgetSettingsRepository: WidgetSettingsRepository
    private lateinit var mockGetNowInstant: () -> Instant
    private lateinit var mockWidgetLoadingStateNotifier: WidgetLoadingStateNotifier
    private lateinit var mockUpdateWidget: (Widget.WidgetId) -> Unit

    @Before
    fun setUp() {
        mockWordsRepository = mockk()
        mockWidgetSettingsRepository = mockk()
        mockGetNowInstant = mockk()
        mockWidgetLoadingStateNotifier = mockk()
        mockUpdateWidget = mockk()
        synchronizer = DefaultWordsSynchronizer(
            wordsRepository =  mockWordsRepository,
            widgetSettingsRepository = mockWidgetSettingsRepository,
            getNowInstant = mockGetNowInstant,
            widgetLoadingStateNotifier = mockWidgetLoadingStateNotifier,
            updateWidget = mockUpdateWidget
        )
        coEvery { mockWidgetSettingsRepository.observeSettings(any()) } returns flowOf(randomWidget())
        coEvery { mockWordsRepository.synchronizeWords(any()) } just runs
        coEvery { mockWidgetSettingsRepository.updateLastUpdatedAt(any(), any()) } just runs
        coEvery { mockWidgetLoadingStateNotifier.setIsWidgetLoading(any()) } just runs
        every { mockGetNowInstant() } returns Instant.now()
        every { mockUpdateWidget(any()) } just runs
        coEvery { mockWordsRepository.deleteCachedWords(any()) } just runs
    }

    @Test
    fun `when words are synchronized_given widget settings do not exist_then exception is thrown`() = runTest {
        val widget = randomWidget()
        coEvery { mockWidgetSettingsRepository.observeSettings(widget.id) } returns flowOf(null)
        var exceptionThrown = false

        try {
            synchronizer.synchronizeWords(widget.id)
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
    }

    @Test
    @Parameters
    fun `when words are synchronized_given widget settings exist_then all dependencies are executed in the correct order`() = runTest {
        synchronizer.synchronizeWords(randomWidgetId())

        coVerifySequence {
            mockWidgetSettingsRepository.observeSettings(any())
            mockWordsRepository.deleteCachedWords(any())
            mockUpdateWidget(any())
            mockWidgetLoadingStateNotifier.setIsWidgetLoading(any())
            mockWordsRepository.synchronizeWords(any())
            mockWidgetSettingsRepository.updateLastUpdatedAt(any(), any())
        }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then cached words are deleted from repository`() = runTest {
        val widgetId = randomWidgetId()

        synchronizer.synchronizeWords(widgetId)

        coVerify { mockWordsRepository.deleteCachedWords(widgetId) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then widget is updated`() = runTest {
        val widgetId = randomWidgetId()

        synchronizer.synchronizeWords(widgetId)

        coVerify { mockUpdateWidget(widgetId) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then widget loading state is triggered`() = runTest {
        val widgetId = randomWidgetId()

        synchronizer.synchronizeWords(widgetId)

        coVerify { mockWidgetLoadingStateNotifier.setIsWidgetLoading(widgetId) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then words are synchronized in the repository`() = runTest {
        val widget = randomWidget()
        val widgetId = widget.id
        val expectedSyncRequest = widget.run { WordsRepository.SynchronizationRequest(id, spreadsheetId, sheetId) }
        coEvery { mockWidgetSettingsRepository.observeSettings(widgetId) } returns flowOf(widget)

        synchronizer.synchronizeWords(widgetId)

        coVerify { mockWordsRepository.synchronizeWords(expectedSyncRequest) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then last update date of the correct widget is updated`() = runTest {
        val widgetId = randomWidgetId()
        val currentTime = Instant.now()
        every { mockGetNowInstant.invoke() } returns currentTime

        synchronizer.synchronizeWords(widgetId)

        coVerify { mockWidgetSettingsRepository.updateLastUpdatedAt(widgetId, currentTime) }
    }
}