package com.pt.glancewords.data.repository

import com.pt.glancewords.data.datasource.CSVLine
import com.pt.glancewords.data.datasource.WordsLocalDataSource
import com.pt.glancewords.data.datasource.WordsRemoteDataSource
import com.pt.glancewords.data.fixture.SHEET_SPREADSHEET_ID
import com.pt.glancewords.data.fixture.WIDGET_ID
import com.pt.glancewords.data.fixture.WORD_PAIR
import com.pt.glancewords.data.mapper.WordPairMapper
import com.pt.glancewords.domain.model.WordPair
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.testcommon.coroutines.collectToListInBackground
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

    private val everyGetLocalWords get() = coEvery { fakeLocalDataSource.getWords(WIDGET_ID) }
    private val everyGetRemoteWords get() = coEvery { fakeRemoteDataSource.getWords(SHEET_SPREADSHEET_ID) }
    private val everyStoreLocalWords get() = coEvery { fakeLocalDataSource.storeWords(WIDGET_ID, TEST_CSV_LINES) }
    private val everyMapWordPair get() = every { fakeWordPairMapper.map(TEST_CSV_LINES.first()) }
    private val everyDeleteLocalWords get() = coEvery { fakeLocalDataSource.deleteWords(WIDGET_ID) }

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

        assertEquals(listOf(WORD_PAIR), words.single())
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

        repository.synchronizeWords(
            WordsRepository.SynchronizationRequest(
                WIDGET_ID,
                SHEET_SPREADSHEET_ID
            )
        )

        assertEquals(listOf(WORD_PAIR), words.single())
    }

    @Test
    fun `when words are synchronized_new words are stored`() = runTest(dispatcher) {
        remoteWordsAreReturned()
        wordsStorageIsSuspended()

        backgroundScope.launch {
            repository.synchronizeWords(
                WordsRepository.SynchronizationRequest(WIDGET_ID, SHEET_SPREADSHEET_ID)
            )
        }

        coVerify { fakeLocalDataSource.storeWords(WIDGET_ID, TEST_CSV_LINES) }
    }

    @Test
    fun `when words are deleted_they are deleted from the local data source`() = runTest(dispatcher) {
        localWordsAreDeleted()

        repository.deleteCachedWords(WIDGET_ID)

        coVerify { fakeLocalDataSource.deleteWords(WIDGET_ID) }
    }

    private fun TestScope.collectWords(): List<List<WordPair>?> =
        collectToListInBackground(repository.observeWords(WIDGET_ID))

    private fun noLocalWordsAreReturned() = everyGetLocalWords returns null
    private fun localWordsAreReturned() = everyGetLocalWords returns TEST_CSV_LINES
    private fun remoteWordsAreReturned() = everyGetRemoteWords returns TEST_CSV_LINES
    private fun wordsAreStored() = everyStoreLocalWords just runs
    private fun wordsStorageIsSuspended() = everyStoreLocalWords just awaits
    private fun mapperReturnsWordPair() = everyMapWordPair returns WORD_PAIR
    private fun localWordsAreDeleted() = everyDeleteLocalWords just runs

    companion object {
        private val TEST_CSV_LINES = listOf(CSVLine("a,b"))
    }
}