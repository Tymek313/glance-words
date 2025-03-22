package com.pt.glancewords.data.repository

import com.pt.glancewords.data.datasource.WordsLocalDataSource
import com.pt.glancewords.data.datasource.WordsRemoteDataSource
import com.pt.glancewords.data.fixture.SHEET_SPREADSHEET_ID
import com.pt.glancewords.data.fixture.randomSheetId
import com.pt.glancewords.domain.model.WordPair
import com.pt.testcommon.coroutines.collectToListInBackground
import com.pt.testcommon.fixture.randomString
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

    private val everyGetLocalWords get() = coEvery { fakeLocalDataSource.observeWords(SHEET_ID) }
    private val everyGetRemoteWords get() = coEvery { fakeRemoteDataSource.getWords(SHEET_SPREADSHEET_ID) }
    private val everyStoreLocalWords get() = coEvery { fakeLocalDataSource.storeWords(SHEET_ID, WORD_PAIRS) }

    @Before
    fun setUp() {
        fakeLocalDataSource = mockk()
        fakeRemoteDataSource = mockk()
        repository = DefaultWordsRepository(fakeRemoteDataSource, fakeLocalDataSource)
    }

    @Test
    fun `when words are observed_then stored words are emitted`() = runTest(dispatcher) {
        localWordsAreReturned()

        val words = collectToListInBackground(repository.observeWords(SHEET_ID))

        assertEquals(WORD_PAIRS, words.single())
    }

    @Test
    fun `when words are synchronized_given remote fetch succeeds_then true is returned`() = runTest(dispatcher) {
        remoteWordsAreFetched()
        wordsAreStored()

        val syncSucceeded = repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)

        assertTrue(syncSucceeded)
    }

    @Test
    fun `when words are synchronized_given remote fetch succeeds_then new words are stored`() = runTest(dispatcher) {
        remoteWordsAreFetched()
        wordsStorageIsSuspended()

        backgroundScope.launch {
            repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)
        }

        coVerify { fakeLocalDataSource.storeWords(SHEET_ID, WORD_PAIRS) }
    }

    @Test
    fun `when words are synchronized_given remote fetch fails_then false is returned`() = runTest(dispatcher) {
        remoteWordsFetchFails()

        val syncSucceeded = repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)

        assertFalse(syncSucceeded, "Synchronization succeeded")
    }

    @Test
    fun `when words are synchronized_given remote fetch fails_then no words are stored`() = runTest(dispatcher) {
        remoteWordsFetchFails()

        repository.synchronizeWords(SHEET_ID, SHEET_SPREADSHEET_ID)

        coVerify(inverse = true) { fakeLocalDataSource.storeWords(any(), any()) }
    }


    private fun localWordsAreReturned() = everyGetLocalWords returns flowOf(WORD_PAIRS)
    private fun remoteWordsAreFetched() = everyGetRemoteWords returns WORD_PAIRS
    private fun remoteWordsFetchFails() = everyGetRemoteWords returns null
    private fun wordsAreStored() = everyStoreLocalWords just runs
    private fun wordsStorageIsSuspended() = everyStoreLocalWords just awaits

    private companion object {
        val WORD_PAIRS = listOf(WordPair(original = randomString(), translated = randomString()))
        val SHEET_ID = randomSheetId()
    }
}