package com.example.words.repository

import com.example.words.datasource.CSVLine
import com.example.words.datasource.FileWordsLocalDataSource
import com.example.words.model.Widget
import com.example.words.randomWidgetId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.BufferedSource
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FileWordsLocalDataSourceTest {

    private lateinit var dataSource: FileWordsLocalDataSource
    private lateinit var fakeFileSystem: FakeFileSystem
    private val spreadsheetsDirectory = "spreadsheets".toPath()
    private val dispatcher = UnconfinedTestDispatcher()
    private var widgetId by Delegates.notNull<Widget.WidgetId>()
    private lateinit var targetFilePath: Path

    @Before
    fun setUp() {
        widgetId = randomWidgetId()
        targetFilePath = spreadsheetsDirectory / "${widgetId.value}.csv"
        fakeFileSystem = FakeFileSystem()
        dataSource = FileWordsLocalDataSource(fakeFileSystem, spreadsheetsDirectory, dispatcher)
    }

    @Test
    fun `when words are requested_given file exists_word lines are returned`() = runTest(dispatcher) {
        fakeFileSystem.createDirectory(spreadsheetsDirectory)
        fakeFileSystem.write(targetFilePath, mustCreate = true) { writeUtf8(TEST_CSV) }

        val words = dataSource.getWords(widgetId)

        assertEquals(TEST_CSV_LINES, words)
    }

    @Test
    fun `when words are requested_given file does not exist_null is returned`() = runTest(dispatcher) {
        val words = dataSource.getWords(widgetId)

        assertNull(words)
    }

    @Test
    fun `when words are stored_given file exists_words should be written to the file`() = runTest(dispatcher) {
        fakeFileSystem.createDirectory(spreadsheetsDirectory)
        fakeFileSystem.write(targetFilePath, mustCreate = true) { emit() }

        dataSource.storeWords(widgetId, TEST_CSV_LINES)

        val fileContent = fakeFileSystem.read(targetFilePath, BufferedSource::readUtf8)
        assertEquals(TEST_CSV, fileContent)
    }

    @Test
    fun `when words are stored_given file does not exist_file should be created and words written to it`() = runTest(dispatcher) {
        dataSource.storeWords(widgetId, TEST_CSV_LINES)

        val fileContent = fakeFileSystem.read(targetFilePath, BufferedSource::readUtf8)
        assertEquals(TEST_CSV, fileContent)
    }

    @Test
    fun `when words are deleted_words should be deleted from the filesystem`() = runTest(dispatcher) {
        fakeFileSystem.createDirectory(spreadsheetsDirectory)
        fakeFileSystem.write(targetFilePath, mustCreate = true) { emit() }

        dataSource.deleteWords(widgetId)

        val fileExists = fakeFileSystem.exists(targetFilePath)
        assertFalse(fileExists)
    }

    companion object {
        private const val TEST_CSV = "a,b\r\nc,d"
        private val TEST_CSV_LINES = listOf(CSVLine("a,b"), CSVLine("c,d"))
    }
}