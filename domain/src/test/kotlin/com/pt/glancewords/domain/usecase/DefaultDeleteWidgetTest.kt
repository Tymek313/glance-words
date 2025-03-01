package com.pt.glancewords.domain.usecase

import com.pt.glancewords.domain.fixture.randomSheetId
import com.pt.glancewords.domain.fixture.randomWidgetId
import com.pt.glancewords.domain.model.Sheet
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.Widget
import com.pt.glancewords.domain.repository.SheetRepository
import com.pt.glancewords.domain.repository.WidgetRepository
import com.pt.glancewords.domain.repository.WordsRepository
import com.pt.testcommon.fixture.randomInstant
import com.pt.testcommon.fixture.randomInt
import com.pt.testcommon.fixture.randomString
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DefaultDeleteWidgetTest {

    private lateinit var deleteWidget: DefaultDeleteWidget
    private lateinit var widgetRepository: WidgetRepository
    private lateinit var sheetRepository: SheetRepository
    private lateinit var wordsRepository: WordsRepository

    private val everyGetWidget get() = coEvery { widgetRepository.getWidget(WIDGET.id) }
    private val everyDeleteWidget get() = coEvery { widgetRepository.deleteWidget(WIDGET.id) }
    private val everySheetExists get() = coEvery { sheetRepository.exists(SHEET.id) }
    private val everyDeleteWords get() = coEvery { wordsRepository.deleteWords(SHEET.id) }

    private fun widgetExists() = everyGetWidget returns WIDGET
    private fun widgetDoesNotExist() = everyGetWidget returns null
    private fun widgetDeletionIsSuspended() = everyDeleteWidget just awaits
    private fun widgetIsDeleted() = everyDeleteWidget just runs
    private fun sheetExists() = everySheetExists returns true
    private fun sheetDoesNotExist() = everySheetExists returns false
    private fun wordsAreDeleted() = everyDeleteWords just runs

    @Before
    fun setup() {
        widgetRepository = mockk()
        sheetRepository = mockk()
        wordsRepository = mockk()
        deleteWidget = DefaultDeleteWidget(widgetRepository, sheetRepository, wordsRepository, mockk(relaxed = true))
    }

    @Test
    fun `when invoked_given widget does not exist_then deletion is aborted`() = runTest {
        widgetDoesNotExist()

        deleteWidget(WIDGET.id)

        coVerify(inverse = true) {
            widgetRepository.getWidget(any())
            widgetRepository.deleteWidget(any())
            sheetRepository.exists(any())
            wordsRepository.deleteWords(any())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when invoked_given widget exists_then widget is deleted`() = runTest(UnconfinedTestDispatcher()) {
        widgetExists()
        widgetDeletionIsSuspended()

        backgroundScope.launch {
            deleteWidget(WIDGET.id)
        }

        coVerify { widgetRepository.deleteWidget(WIDGET.id) }
    }

    @Test
    fun `when invoked_given widget exists and sheet still exists_then associated words are not deleted`() = runTest {
        widgetExists()
        widgetIsDeleted()
        sheetExists()

        deleteWidget(WIDGET.id)

        coVerify(inverse = true) { wordsRepository.deleteWords(any()) }
    }

    @Test
    fun `when invoked_given widget exists and sheet has been removed_then associated words are deleted`() = runTest {
        widgetExists()
        widgetIsDeleted()
        sheetDoesNotExist()
        wordsAreDeleted()

        deleteWidget(WIDGET.id)

        coVerify { wordsRepository.deleteWords(SHEET.id) }
    }

    private companion object {
        val SHEET = Sheet(
            id = randomSheetId(),
            sheetSpreadsheetId = SheetSpreadsheetId(randomString(), randomInt()),
            name = randomString(),
            lastUpdatedAt = randomInstant()
        )
        val WIDGET = Widget(id = randomWidgetId(), sheet = SHEET)
    }
}