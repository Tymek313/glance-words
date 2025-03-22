package com.pt.glancewords.data.repository

import com.pt.glancewords.data.database.DbWordPairQueries
import com.pt.glancewords.data.database.GetBySheetId
import com.pt.glancewords.data.datasource.DatabaseWordsLocalDataSource
import com.pt.glancewords.data.fixture.randomDbSheet
import com.pt.glancewords.data.utility.createTestDatabase
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.WordPair
import com.pt.testcommon.coroutines.collectToListInBackground
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseWordsLocalDataSourceTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDatabase: DbWordPairQueries
    private lateinit var dataSource: DatabaseWordsLocalDataSource

    @Before
    fun setUp() {
        val database = createTestDatabase()
        fakeDatabase = database.dbWordPairQueries
        dataSource = DatabaseWordsLocalDataSource(fakeDatabase, dispatcher)
        database.dbSheetQueries.insert(randomDbSheet())
    }

    @Test
    fun `when words are observed_given word pairs exist_they are returned`() = runTest(dispatcher) {
        storeWordPairsInDatabase()

        val words = collectToListInBackground(dataSource.observeWords(SHEET_ID))

        assertEquals(WORD_PAIRS, words.first())
    }

    @Test
    fun `when words are stored_given word pairs are not stored in database yet_they are stored in database`() = runTest(dispatcher) {
        dataSource.storeWords(SHEET_ID, WORD_PAIRS)

        assertEquals(
            DB_WORD_PAIRS,
            fakeDatabase.getBySheetId(SHEET_ID.value).executeAsList()
        )
    }

    @Test
    fun `when words are stored_given word pairs are already stored in database_they are overwritten`() = runTest(dispatcher) {
        storeWordPairsInDatabase()

        dataSource.storeWords(SHEET_ID, WORD_PAIRS)

        assertEquals(
            DB_WORD_PAIRS,
            fakeDatabase.getBySheetId(SHEET_ID.value).executeAsList()
        )
    }

    @Test
    fun `when words are stored_given they are observed_updated word pairs are emitted`() = runTest(dispatcher) {
        val words = collectToListInBackground(dataSource.observeWords(SHEET_ID))

        dataSource.storeWords(SHEET_ID, WORD_PAIRS)

        assertEquals(WORD_PAIRS, words.last())
    }

    private fun storeWordPairsInDatabase() {
        fakeDatabase.insertOrIgnore(sheet_id = SHEET_ID.value, original = "a", translated = "b")
    }

    private companion object {
        val SHEET_ID = SheetId(1)
        val WORD_PAIRS = listOf(WordPair("a", "b"))
        val DB_WORD_PAIRS = listOf(GetBySheetId(original = "a", translated = "b"))
    }
}