package com.pt.glancewords.domain.usecase

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultAddWidgetTest {

    private lateinit var addWidget: DefaultAddWidget
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeSheetRepository: SheetRepository
    private lateinit var fakeWordsRepository: WordsRepository

    private val everyGetSheet get() = coEvery { fakeSheetRepository.getBySheetSpreadsheetId(SHEET_TO_ADD.sheetSpreadsheetId) }
    private val everyAddSheet get() = coEvery { fakeSheetRepository.addSheet(SHEET_TO_ADD) }
    private val everyDeleteSheet get() = coEvery { fakeSheetRepository.deleteSheet(ADDED_SHEET.id) }
    private val everyAddWidget get() = coEvery { fakeWidgetRepository.addWidget(WIDGET_ID_TO_ADD, any()) }
    private val everySynchronizeWords
        get() = coEvery { fakeWordsRepository.synchronizeWords(ADDED_SHEET.id, SHEET_TO_ADD.sheetSpreadsheetId) }

    private fun wordsAreSynchronized() = everySynchronizeWords returns true
    private fun wordsSynchronizationFails() = everySynchronizeWords returns false
    private fun wordsSynchronizationThrows() = everySynchronizeWords throws Exception()
    private fun wordsSynchronizationIsCancelled() = everySynchronizeWords throws CancellationException()
    private fun sheetExists() = everyGetSheet returns STORED_SHEET
    private fun sheetDoesNotExist() = everyGetSheet returns null
    private fun sheetIsAdded() = everyAddSheet returns ADDED_SHEET
    private fun sheetAdditionThrows() = everyAddSheet throws Exception()
    private fun sheetIsDeleted() = everyDeleteSheet just runs
    private fun widgetIsAdded() = everyAddWidget just runs

    @Before
    fun setup() {
        fakeWidgetRepository = mockk()
        fakeSheetRepository = mockk()
        fakeWordsRepository = mockk()
        addWidget = DefaultAddWidget(
            widgetRepository = fakeWidgetRepository,
            sheetRepository = fakeSheetRepository,
            wordsRepository = fakeWordsRepository,
            logger = mockk(relaxed = true)
        )
    }

    @Test
    fun `when invoked_given sheet exists_then widget with the existing sheet id is added`() = runTest {
        sheetExists()
        widgetIsAdded()
        wordsAreSynchronized()

        addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        coVerify { fakeWidgetRepository.addWidget(WIDGET_ID_TO_ADD, STORED_SHEET.id) }
    }

    @Test
    fun `when invoked_given sheet exists_then true is returned`() = runTest {
        sheetExists()
        widgetIsAdded()
        wordsAreSynchronized()

        val result = addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        assertTrue(result)
    }

    @Test
    fun `when invoked_given sheet does not exist and words synchronization succeeds_then true is returned`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        widgetIsAdded()
        wordsAreSynchronized()

        val syncSucceeded = addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        assertTrue(syncSucceeded)
    }

    @Test
    fun `when invoked_given sheet does not exist and words synchronization succeeds_then widget with the newly stored sheet id is added`() =
        runTest {
            sheetDoesNotExist()
            sheetIsAdded()
            widgetIsAdded()
            wordsAreSynchronized()

            addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

            coVerify { fakeWidgetRepository.addWidget(WIDGET_ID_TO_ADD, ADDED_SHEET.id) }
        }

    @Test
    fun `when invoked_given sheet does not exist and words synchronization fails_then false is returned`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        widgetIsAdded()
        wordsSynchronizationFails()
        sheetIsDeleted()

        val syncSucceeded = addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        assertFalse(syncSucceeded)
    }

    @Test
    fun `when invoked_given sheet does not exist and words synchronization fails_then widget is not added`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        wordsSynchronizationFails()
        sheetIsDeleted()

        addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        coVerify(inverse = true) { fakeWidgetRepository.addWidget(any(), any()) }
    }

    @Test
    fun `when invoked_given sheet does not exist and words synchronization fails_then sheet is deleted`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        wordsSynchronizationFails()
        sheetIsDeleted()

        addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        coVerify { fakeSheetRepository.deleteSheet(ADDED_SHEET.id) }
    }

    @Test
    fun `when invoked_given sheet does not exist and exception is thrown after sheet is added_then added sheet is deleted`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        wordsSynchronizationThrows()
        sheetIsDeleted()

        addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        coVerify { fakeSheetRepository.deleteSheet(ADDED_SHEET.id) }
    }

    @Test
    fun `when invoked_given sheet does not exist and exception is thrown after sheet is added_then false is returned`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        wordsSynchronizationThrows()
        sheetIsDeleted()

        val syncSucceeded = addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        assertFalse(syncSucceeded)
    }

    @Test
    fun `when invoked_given sheet does not exist and exception is thrown before sheet is added_then no sheet is not deleted`() = runTest {
        sheetDoesNotExist()
        sheetAdditionThrows()
        sheetIsDeleted()

        addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)

        coVerify(inverse = true) { fakeSheetRepository.deleteSheet(any()) }
    }

    @Test(expected = CancellationException::class)
    fun `when invoked_given sheet does not exist and coroutine is cancelled after sheet is added_then exception is rethrown`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        wordsSynchronizationIsCancelled()
        sheetIsDeleted()

        addWidget(WIDGET_ID_TO_ADD, SHEET_TO_ADD)
    }

    private companion object {
        val WIDGET_ID_TO_ADD = WidgetId(randomInt())
        val SHEET_TO_ADD = NewSheet(
            sheetSpreadsheetId = SheetSpreadsheetId(spreadsheetId = randomString(), sheetId = randomInt()),
            name = randomString()
        )
        val STORED_SHEET = randomSheet()
        val ADDED_SHEET = randomSheet()
        fun randomSheet() = Sheet(
            id = SheetId(randomInt()),
            sheetSpreadsheetId = SheetSpreadsheetId(spreadsheetId = randomString(), sheetId = randomInt()),
            name = randomString(),
            lastUpdatedAt = null
        )
    }
}