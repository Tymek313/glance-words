package com.pt.glancewords.data.repository

import com.pt.glancewords.data.datasource.CSVLine
import com.pt.glancewords.data.datasource.WordsLocalDataSource
import com.pt.glancewords.data.datasource.WordsRemoteDataSource
import com.pt.glancewords.data.fixture.SHEET_SPREADSHEET_ID
import com.pt.glancewords.data.fixture.WORD_PAIR
import com.pt.glancewords.data.fixture.randomSheetId
import com.pt.glancewords.data.mapper.WordPairMapper
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWordsRepositoryTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: DefaultWordsRepository
    private lateinit var fakeLocalDataSource: WordsLocalDataSource
    private lateinit var fakeRemoteDataSource: WordsRemoteDataSource
    private lateinit var fakeWordPairMapper: WordPairMapper

    private val everyGetLocalWords get() = coEvery { fakeLocalDataSource.getWords(SHEET_ID) }
    private val everyGetRemoteWords get() = coEvery { fakeRemoteDataSource.getWords(SHEET_SPREADSHEET_ID) }
    private val everyStoreLocalWords get() = coEvery { fakeLocalDataSource.storeWords(SHEET_ID, TEST_CSV_LINES) }
    private val everyMapWordPair get() = every { fakeWordPairMapper.map(TEST_CSV_LINES.first()) }
    private val everyDeleteWords get() = coEvery { fakeLocalDataSource.deleteWords(SHEET_ID) }

    @Before
    fun setUp() {
        fakeLocalDataSource = mockk()
        fakeRemoteDataSource = mockk()
        fakeWordPairMapper = mockk()
        repository = DefaultWordsRepository(fakeRemoteDataSource, fakeLocalDataSource, fakeWordPairMapper)
    }

    @Test
    fun `when words are observed_given there are cached words for the widget_then they are emitted`() = runTest(dispatcher) {
        localWordsAreReturned()
        mapperReturnsWordPair()

        val words = collectToListInBackground(repository.observeWords(SHEET_ID))

        assertEquals(listOf(WORD_PAIR), words.single())
    }

    @Test
    fun `when words are observed_given there are no cached words for the widget_then no words are emitted`() = runTest(dispatcher) {
        noLocalWordsAreReturned()

        val words = collectToListInBackground(repository.observeWords(SHEET_ID))

        assertTrue(words.isEmpty())
    }

    @Test
    fun `when words are synchronized_given remote fetch succeeds_then true is returned`() = runTest(dispatcher) {
        remoteWordsAreFetched()
        wordsAreStored()
        mapperReturnsWordPair()

        val syncSucceeded = repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)

        assertTrue(syncSucceeded)
    }

    @Test
    fun `when words are synchronized_given remote fetch succeeds_then new words are emitted`() = runTest(dispatcher) {
        noLocalWordsAreReturned()
        remoteWordsAreFetched()
        wordsAreStored()
        mapperReturnsWordPair()
        val words = collectToListInBackground(repository.observeWords(SHEET_ID))

        repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)

        assertEquals(listOf(WORD_PAIR), words.single())
    }

    @Test
    fun `when words are synchronized_given remote fetch succeeds_then new words are stored`() = runTest(dispatcher) {
        remoteWordsAreFetched()
        wordsStorageIsSuspended()

        backgroundScope.launch {
            repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)
        }

        coVerify { fakeLocalDataSource.storeWords(SHEET_ID, TEST_CSV_LINES) }
    }

    @Test
    fun `when words are synchronized_given remote fetch fails_then false is returned`() = runTest(dispatcher) {
        remoteWordsFetchFails()

        val syncSucceeded = repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)

        assertFalse(syncSucceeded, "Synchronization succeeded")
    }

    @Test
    fun `when words are deleted_then they are deleted from the local data source`() = runTest(dispatcher) {
        localWordsAreDeleted()

        repository.deleteWords(SHEET_ID)

        coVerify { fakeLocalDataSource.deleteWords(SHEET_ID) }
    }

    private fun noLocalWordsAreReturned() = everyGetLocalWords returns null
    private fun localWordsAreReturned() = everyGetLocalWords returns TEST_CSV_LINES
    private fun remoteWordsAreFetched() = everyGetRemoteWords returns TEST_CSV_LINES
    private fun remoteWordsFetchFails() = everyGetRemoteWords returns null
    private fun wordsAreStored() = everyStoreLocalWords just runs
    private fun wordsStorageIsSuspended() = everyStoreLocalWords just awaits
    private fun mapperReturnsWordPair() = everyMapWordPair returns WORD_PAIR
    private fun localWordsAreDeleted() = everyDeleteWords just runs

    private companion object {
        val TEST_CSV_LINES = listOf(CSVLine("a,b"))
        val SHEET_ID = randomSheetId()
    }
}