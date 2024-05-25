package com.example.words.repository

import com.example.words.datasource.CSVLine
import com.example.words.datasource.FileWordsLocalDataSource
import com.example.words.model.Widget
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.BufferedSource
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@MockKExtension.ConfirmVerification
class FileWordsLocalDataSourceTest {

    private lateinit var dataSource: FileWordsLocalDataSource
    private lateinit var fakeFileSystem: FakeFileSystem
    private val spreadsheetsDirectory = "spreadsheets".toPath()
    private val dispatcher = UnconfinedTestDispatcher()
    private var widgetId by Delegates.notNull<Widget.WidgetId>()

    @Before
    fun setUp() {
        widgetId = getRandomWidgetId()
        fakeFileSystem = FakeFileSystem()
        dataSource = FileWordsLocalDataSource(fakeFileSystem, spreadsheetsDirectory, dispatcher)
    }

    @Test
    fun `when words are requested_given file exists_word lines are returned`() = runTest(dispatcher) {
        fakeFileSystem.createDirectory(spreadsheetsDirectory)
        fakeFileSystem.write(spreadsheetsDirectory / "${widgetId.value}.csv") { writeUtf8(TEST_CSV) }

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
        fakeFileSystem.write(spreadsheetsDirectory / "${widgetId.value}.csv") { emit() }

        dataSource.storeWords(widgetId, TEST_CSV_LINES)

        val fileContent = fakeFileSystem.read(spreadsheetsDirectory / "${widgetId.value}.csv", BufferedSource::readUtf8)
        assertEquals(TEST_CSV, fileContent)
    }

    @Test
    fun `when words are stored_given file does not exist_words should be written to the file`() = runTest(dispatcher) {
        dataSource.storeWords(widgetId, TEST_CSV_LINES)

        val fileContent = fakeFileSystem.read(spreadsheetsDirectory / "${widgetId.value}.csv", BufferedSource::readUtf8)
        assertEquals(TEST_CSV, fileContent)
    }

    companion object {
        private const val TEST_CSV = "a,b\r\nc,d"
        private val TEST_CSV_LINES = listOf(CSVLine("a,b"), CSVLine("c,d"))

        fun getRandomWidgetId() = Widget.WidgetId(Random.nextInt())
    }
}