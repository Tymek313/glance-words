package com.pt.glancewords.data.repository

import com.pt.glancewords.data.datasource.CSVLine
import com.pt.glancewords.data.datasource.FileWordsLocalDataSource
import com.pt.glancewords.data.fixture.randomSheetId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.BufferedSource
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FileWordsLocalDataSourceTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeFileSystem: FakeFileSystem
    private lateinit var dataSource: FileWordsLocalDataSource

    @Before
    fun setUp() {
        fakeFileSystem = FakeFileSystem()
        dataSource = FileWordsLocalDataSource(fakeFileSystem, SPREADSHEETS_DIRECTORY, dispatcher)
    }

    @Test
    fun `when words are requested_given file exists_word lines are returned`() = runTest(dispatcher) {
        createFilledCSVFile()

        val words = dataSource.getWords(SHEET_ID)

        assertEquals(TEST_CSV_LINES, words)
    }

    @Test
    fun `when words are requested_given file does not exist_null is returned`() = runTest(dispatcher) {
        val words = dataSource.getWords(SHEET_ID)

        assertNull(words)
    }

    @Test
    fun `when words are stored_given file exists_words should be written to the file`() = runTest(dispatcher) {
        createEmptyCSVFile()

        dataSource.storeWords(SHEET_ID, TEST_CSV_LINES)

        val fileContent = fakeFileSystem.read(TARGET_FILE_PATH, BufferedSource::readUtf8)
        assertEquals(TEST_CSV, fileContent)
    }

    @Test
    fun `when words are stored_given file does not exist_file should be created and words written to it`() = runTest(dispatcher) {
        dataSource.storeWords(SHEET_ID, TEST_CSV_LINES)

        val fileContent = fakeFileSystem.read(TARGET_FILE_PATH, BufferedSource::readUtf8)
        assertEquals(TEST_CSV, fileContent)
    }

    @Test
    fun `when words are deleted_words should be deleted from the filesystem`() = runTest(dispatcher) {
        createEmptyCSVFile()

        dataSource.deleteWords(SHEET_ID)

        val fileExists = fakeFileSystem.exists(TARGET_FILE_PATH)
        assertFalse(fileExists)
    }

    private fun createFilledCSVFile() {
        fakeFileSystem.createDirectory(SPREADSHEETS_DIRECTORY)
        fakeFileSystem.write(TARGET_FILE_PATH, mustCreate = true) { writeUtf8(TEST_CSV) }
    }

    private fun createEmptyCSVFile() {
        fakeFileSystem.createDirectory(SPREADSHEETS_DIRECTORY)
        fakeFileSystem.write(TARGET_FILE_PATH, mustCreate = true) { emit() }
    }

    private companion object {
        const val TEST_CSV = "a,b\r\nc,d"
        val TEST_CSV_LINES = listOf(CSVLine("a,b"), CSVLine("c,d"))
        val SPREADSHEETS_DIRECTORY = "spreadsheets".toPath()
        val SHEET_ID = randomSheetId()
        val TARGET_FILE_PATH = SPREADSHEETS_DIRECTORY / "${SHEET_ID.value}.csv"
    }
}