package com.pt.domain.words.usecase

import com.pt.glancewords.domain.model.NewSheet
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WidgetId
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.glancewords.domain.usecase.AddWidget
import com.pt.glancewords.domain.usecase.DefaultAddWidget
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultAddWidgetTest {

    private lateinit var addWidget: DefaultAddWidget
    private lateinit var fakeWidgetRepository: WidgetRepository
    private lateinit var fakeSheetRepository: SheetRepository
    private lateinit var fakeWordsRepository: WordsRepository

    private val everyGetSheet get() = coEvery { fakeSheetRepository.getBySheetSpreadsheetId(WIDGET_TO_ADD.sheet.sheetSpreadsheetId) }
    private val everyAddSheet get() = coEvery { fakeSheetRepository.addSheet(WIDGET_TO_ADD.sheet) }
    private val everyAddWidget get() = coEvery { fakeWidgetRepository.addWidget(WIDGET_TO_ADD.widgetId, any()) }
    private val everySynchronizeWords get() = coEvery { fakeWordsRepository.synchronizeWords(WORDS_SYNCHRONIZATION_REQUEST) }

    private fun wordsAreSynchronized() = everySynchronizeWords returns true
    private fun wordsSynchronizationFails() = everySynchronizeWords returns false
    private fun sheetIsRetrieved() = everyGetSheet returns STORED_SHEET
    private fun sheetDoesNotExist() = everyGetSheet returns null
    private fun sheetIsAdded() = everyAddSheet returns ADDED_SHEET
    private fun widgetIsAdded() = everyAddWidget just runs

    @Before
    fun setup() {
        fakeWidgetRepository = mockk()
        fakeSheetRepository = mockk()
        fakeWordsRepository = mockk()
        addWidget = DefaultAddWidget(fakeWidgetRepository, fakeSheetRepository, fakeWordsRepository)
    }

    @Test
    fun `when invoked_given words synchronization succeeds_then true is returned`() = runTest {
        sheetIsRetrieved()
        widgetIsAdded()
        wordsAreSynchronized()

        val syncSucceeded = addWidget(WIDGET_TO_ADD)

        assertTrue(syncSucceeded)
    }

    @Test
    fun `when invoked_given words synchronization succeeds and sheet exists_then widget with the existing sheet id is added`() = runTest {
        sheetIsRetrieved()
        widgetIsAdded()
        wordsAreSynchronized()

        addWidget(WIDGET_TO_ADD)

        coVerify { fakeWidgetRepository.addWidget(WIDGET_TO_ADD.widgetId, STORED_SHEET.id) }
    }

    @Test
    fun `when invoked_given words synchronization succeeds and sheet does not exist_then widget with the newly stored sheet id is added`() = runTest {
        sheetDoesNotExist()
        sheetIsAdded()
        widgetIsAdded()
        wordsAreSynchronized()

        addWidget(WIDGET_TO_ADD)

        coVerify { fakeWidgetRepository.addWidget(WIDGET_TO_ADD.widgetId, ADDED_SHEET.id) }
    }

    @Test
    fun `when invoked_given words synchronization fails_then false is returned`() = runTest {
        sheetIsRetrieved()
        widgetIsAdded()
        wordsSynchronizationFails()

        val syncSucceeded = addWidget(WIDGET_TO_ADD)

        assertFalse(syncSucceeded)
    }

    @Test
    fun `when invoked_given words synchronization fails_then sheet is not added`() = runTest {
        wordsSynchronizationFails()

        addWidget(WIDGET_TO_ADD)

        coVerify(inverse = true) { fakeSheetRepository.addSheet(any()) }
    }

    @Test
    fun `when invoked_given words synchronization fails_then widget is not added`() = runTest {
        wordsSynchronizationFails()

        addWidget(WIDGET_TO_ADD)

        coVerify(inverse = true) { fakeWidgetRepository.addWidget(any(), any()) }
    }

    private companion object {
        val WIDGET_TO_ADD = AddWidget.WidgetToAdd(
            widgetId = WidgetId(randomInt()),
            sheet = NewSheet(
                sheetSpreadsheetId = SheetSpreadsheetId(spreadsheetId = randomString(), sheetId = randomInt()),
                name = randomString()
            )
        )
        val STORED_SHEET = randomSheet()
        val ADDED_SHEET = randomSheet()
        val WORDS_SYNCHRONIZATION_REQUEST = WordsRepository.SynchronizationRequest(WIDGET_TO_ADD.widgetId, WIDGET_TO_ADD.sheet.sheetSpreadsheetId)
        fun randomSheet() = Sheet(
            id = SheetId(randomInt()),
            sheetSpreadsheetId = SheetSpreadsheetId(spreadsheetId = randomString(), sheetId = randomInt()),
            name = randomString(),
            lastUpdatedAt = null
        )
    }
}