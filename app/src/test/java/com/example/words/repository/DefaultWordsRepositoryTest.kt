package com.example.words.repository

import com.example.words.coroutines.collectToListInBackground
import com.example.words.datasource.CSVLine
import com.example.words.datasource.WordsLocalDataSource
import com.example.words.datasource.WordsRemoteDataSource
import com.example.words.mapper.WordPairMapper
import com.example.words.model.Widget
import com.example.words.model.WordPair
import com.example.words.randomInt
import com.example.words.randomString
import com.example.words.randomWidgetId
import com.example.words.randomWordPair
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@MockKExtension.ConfirmVerification
class DefaultWordsRepositoryTest {

    private lateinit var repository: DefaultWordsRepository
    private lateinit var fakeLocalDataSource: WordsLocalDataSource
    private lateinit var fakeRemoteDataSource: WordsRemoteDataSource
    private lateinit var fakeWordPairMapper: WordPairMapper

    @Before
    fun setUp() {
        fakeLocalDataSource = mockk()
        fakeRemoteDataSource = mockk()
        fakeWordPairMapper = mockk()
        repository = DefaultWordsRepository(fakeRemoteDataSource, fakeLocalDataSource, fakeWordPairMapper)
    }

    @Test
    fun `when words are observed_given there are cached words for the widget_words stored locally`() = runTest(UnconfinedTestDispatcher()) {
        val widgetId = randomWidgetId()
        val wordPair = randomWordPair()
        coEvery { fakeLocalDataSource.getWords(widgetId) } returns TEST_CSV_LINE
        every { fakeWordPairMapper.map(TEST_CSV_LINE.first()) } returns wordPair

        val words = collectWords(widgetId)

        assertEquals(listOf(wordPair), words.single())
        coVerify {
            fakeLocalDataSource.getWords(widgetId)
            fakeWordPairMapper.map(TEST_CSV_LINE.first())
        }
    }

    @Test
    fun `when words are observed_given there are no cached words for the widget_no words are emitted`() = runTest(UnconfinedTestDispatcher()) {
        val widgetId = randomWidgetId()
        coEvery { fakeLocalDataSource.getWords(widgetId) } returns null

        val words = collectWords(widgetId)

        assertTrue(words.isEmpty())
        coVerify {
            fakeLocalDataSource.getWords(widgetId)
        }
    }

    @Test
    fun `when words are synchronized_new words are emitted`() = runTest(UnconfinedTestDispatcher()) {
        val widgetId = randomWidgetId()
        val spreadsheetId = randomString()
        val sheetId = randomInt()
        val wordPair = randomWordPair()

        coEvery { fakeLocalDataSource.getWords(widgetId) } returns null
        coEvery { fakeRemoteDataSource.getWords(spreadsheetId, sheetId) } returns TEST_CSV_LINE
        coEvery { fakeLocalDataSource.storeWords(widgetId, TEST_CSV_LINE) } just runs
        every { fakeWordPairMapper.map(TEST_CSV_LINE.first()) } returns wordPair

        val words = collectWords(widgetId)
        repository.synchronizeWords(WordsRepository.SynchronizationRequest(widgetId, spreadsheetId, sheetId))

        assertEquals(listOf(wordPair), words.single())
        coVerify {
            fakeLocalDataSource.getWords(widgetId)
            fakeRemoteDataSource.getWords(spreadsheetId, sheetId)
            fakeLocalDataSource.storeWords(widgetId, TEST_CSV_LINE)
            fakeWordPairMapper.map(TEST_CSV_LINE.first())
        }
    }

    @Test
    fun `when words are deleted_they are deleted from the local data source`() = runTest(UnconfinedTestDispatcher()) {
        val widgetId = randomWidgetId()
        coEvery { fakeLocalDataSource.deleteWords(widgetId) } just runs

        repository.deleteCachedWords(widgetId)

        coVerify { fakeLocalDataSource.deleteWords(widgetId) }
    }

    private fun TestScope.collectWords(widgetId: Widget.WidgetId): List<List<WordPair>?> = collectToListInBackground(repository.observeWords(widgetId))

    companion object {
        private val TEST_CSV_LINE = listOf(CSVLine("a,b"))
    }
}