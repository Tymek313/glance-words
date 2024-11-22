package com.example.words.repository

import com.example.words.coroutines.collectToListInBackground
import com.example.words.datasource.CSVLine
import com.example.words.datasource.WordsLocalDataSource
import com.example.words.datasource.WordsRemoteDataSource
import com.example.words.fixture.sheetSpreadsheetIdFixture
import com.example.words.fixture.widgetIdFixture
import com.example.words.fixture.wordPairFixture
import com.example.words.mapper.WordPairMapper
import com.example.words.model.WordPair
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWordsRepositoryTest {
    
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: DefaultWordsRepository
    private lateinit var fakeLocalDataSource: WordsLocalDataSource
    private lateinit var fakeRemoteDataSource: WordsRemoteDataSource
    private lateinit var fakeWordPairMapper: WordPairMapper

    private val everyGetLocalWords get() = coEvery { fakeLocalDataSource.getWords(widgetIdFixture) }
    private val everyGetRemoteWords get() = coEvery { fakeRemoteDataSource.getWords(sheetSpreadsheetIdFixture) }
    private val everyStoreLocalWords get() = coEvery { fakeLocalDataSource.storeWords(widgetIdFixture, TEST_CSV_LINES) }
    private val everyMapWordPair get() = every { fakeWordPairMapper.map(TEST_CSV_LINES.first()) }
    private val everyDeleteLocalWords get() = coEvery { fakeLocalDataSource.deleteWords(widgetIdFixture) }

    @Before
    fun setUp() {
        fakeLocalDataSource = mockk()
        fakeRemoteDataSource = mockk()
        fakeWordPairMapper = mockk()
        repository = DefaultWordsRepository(fakeRemoteDataSource, fakeLocalDataSource, fakeWordPairMapper)
    }

    @Test
    fun `when words are observed_given there are cached words for the widget_they are emitted`() = runTest(dispatcher) {
        localWordsAreReturned()
        mapperReturnsWordPair()

        val words = collectWords()

        assertEquals(listOf(wordPairFixture), words.single())
    }

    @Test
    fun `when words are observed_given there are no cached words for the widget_no words are emitted`() = runTest(dispatcher) {
        noLocalWordsAreReturned()

        val words = collectWords()

        assertTrue(words.isEmpty())
    }

    @Test
    fun `when words are synchronized_new words are emitted`() = runTest(dispatcher) {
        noLocalWordsAreReturned()
        remoteWordsAreReturned()
        wordsAreStored()
        mapperReturnsWordPair()
        val words = collectWords()
        
        repository.synchronizeWords(WordsRepository.SynchronizationRequest(widgetIdFixture, sheetSpreadsheetIdFixture))

        assertEquals(listOf(wordPairFixture), words.single())
    }

    @Test
    fun `when words are synchronized_new words are stored`() = runTest(dispatcher) {
        remoteWordsAreReturned()
        wordsStorageIsSuspended()

        backgroundScope.launch {
            repository.synchronizeWords(WordsRepository.SynchronizationRequest(widgetIdFixture, sheetSpreadsheetIdFixture))
        }

        coVerify { fakeLocalDataSource.storeWords(widgetIdFixture, TEST_CSV_LINES) }
    }


    @Test
    fun `when words are deleted_they are deleted from the local data source`() = runTest(dispatcher) {
        localWordsAreDeleted()

        repository.deleteCachedWords(widgetIdFixture)

        coVerify { fakeLocalDataSource.deleteWords(widgetIdFixture) }
    }

    private fun TestScope.collectWords(): List<List<WordPair>?> = collectToListInBackground(repository.observeWords(widgetIdFixture))

    private fun noLocalWordsAreReturned() = everyGetLocalWords returns null
    private fun localWordsAreReturned() = everyGetLocalWords returns TEST_CSV_LINES
    private fun remoteWordsAreReturned() = everyGetRemoteWords returns TEST_CSV_LINES
    private fun wordsAreStored() = everyStoreLocalWords just runs
    private fun wordsStorageIsSuspended() = everyStoreLocalWords just awaits
    private fun mapperReturnsWordPair() = everyMapWordPair returns wordPairFixture
    private fun localWordsAreDeleted() = everyDeleteLocalWords just runs

    companion object {
        private val TEST_CSV_LINES = listOf(CSVLine("a,b"))
    }
}