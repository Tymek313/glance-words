package com.example.words.repository

import com.example.words.getRandomWidget
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertTrue

@MockKExtension.ConfirmVerification
class DefaultWordsSynchronizerTest {

    private lateinit var synchronizer: DefaultWordsSynchronizer
    private lateinit var mockWordsRepository: WordsRepository
    private lateinit var mockWidgetSettingsRepository: WidgetSettingsRepository
    private lateinit var mockGetNowInstant: () -> Instant

    @Before
    fun setUp() {
        mockWordsRepository = mockk()
        mockWidgetSettingsRepository = mockk()
        mockGetNowInstant = mockk()
        synchronizer = DefaultWordsSynchronizer(
            wordsRepository =  mockWordsRepository,
            widgetSettingsRepository = mockWidgetSettingsRepository,
            getNowInstant = mockGetNowInstant
        )
    }

    @Test
    fun `when words are synchronized_given widget settings do not exist_then exception is be thrown`() = runTest {
        val widget = getRandomWidget()
        val currentTime = Instant.now()
        coEvery { mockWidgetSettingsRepository.observeSettings(widget.id) } returns flowOf(null)
        every { mockGetNowInstant.invoke() } returns currentTime
        var exceptionThrown = false

        try {
            synchronizer.synchronizeWords(widget.id)
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }

        assertTrue(exceptionThrown)
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then words are synchronized in the words repository`() = runTest {
        val widget = getRandomWidget()
        val synchronizationRequest = WordsRepository.SynchronizationRequest(
            widgetId = widget.id,
            spreadsheetId = widget.spreadsheetId,
            sheetId = widget.sheetId
        )
        coEvery { mockWidgetSettingsRepository.observeSettings(widget.id) } returns flowOf(widget)
        coEvery { mockWordsRepository.synchronizeWords(synchronizationRequest) } just runs
        coEvery { mockWidgetSettingsRepository.updateLastUpdatedAt(any(), any()) } just runs
        every { mockGetNowInstant.invoke() } returns Instant.now()

        synchronizer.synchronizeWords(widget.id)

        coVerify { mockWordsRepository.synchronizeWords(synchronizationRequest) }
    }

    @Test
    fun `when words are synchronized_given widget settings exist_then widget last update date is updated`() = runTest {
        val widget = getRandomWidget()
        val currentTime = Instant.now()
        coEvery { mockWidgetSettingsRepository.observeSettings(widget.id) } returns flowOf(widget)
        coEvery { mockWordsRepository.synchronizeWords(any()) } just runs
        coEvery { mockWidgetSettingsRepository.updateLastUpdatedAt(widget.id, currentTime) } just runs
        every { mockGetNowInstant.invoke() } returns currentTime

        synchronizer.synchronizeWords(widget.id)

        coVerify { mockWidgetSettingsRepository.updateLastUpdatedAt(widget.id, currentTime) }
    }
}