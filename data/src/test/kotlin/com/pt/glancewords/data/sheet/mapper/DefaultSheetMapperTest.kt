package com.pt.glancewords.data.sheet.mapper

import com.pt.glancewords.data.database.DbSheet
import com.pt.glancewords.data.fixture.randomDbSheet
import com.pt.glancewords.data.fixture.randomSheet
import com.pt.glancewords.domain.sheet.model.NewSheet
import com.pt.glancewords.domain.sheet.model.Sheet
import io.mockk.every
import io.mockk.mockk

import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class DefaultSheetMapperTest {

    private lateinit var fakeGetInstant: () -> Instant
    private lateinit var mapper: DefaultSheetMapper

    private val everyGetInstant get() = every { fakeGetInstant() }

    private fun currentInstantIsReturned() = everyGetInstant returns SHEET.lastUpdatedAt!!

    @Before
    fun setUp() {
        fakeGetInstant = mockk()
        mapper = DefaultSheetMapper(getNowInstant = fakeGetInstant)
    }

    @Test
    fun `when mapping database entity to domain_then mapped sheet is returned`() {
        val result = mapper.mapToDomain(DB_SHEET)

        assertEquals(SHEET, result)
    }

    @Test
    fun `when mapping database entity to domain_given sheet was not updated_then mapped sheet without update timestamp is returned`() {
        val result = mapper.mapToDomain(DB_SHEET.copy(last_updated_at = null))

        assertEquals(SHEET.copy(lastUpdatedAt = null), result)
    }

    @Test
    fun `when mapping new sheet to domain_then mapped sheet is returned`() {
        val result = mapper.mapToDomain(NEW_SHEET, SHEET.id)

        assertEquals(SHEET.copy(lastUpdatedAt = null), result)
    }

    @Test
    fun `when mapping new sheet to database entity_then mapped sheet is returned`() {
        currentInstantIsReturned()

        val result = mapper.mapToDb(NEW_SHEET)

        assertEquals(DB_SHEET.copy(id = -1), result)
    }

    private companion object {
        val SHEET = randomSheet()
        val NEW_SHEET = NewSheet(
            sheetSpreadsheetId = SHEET.sheetSpreadsheetId,
            name = SHEET.name
        )
        val DB_SHEET = SHEET.run {
            DbSheet(
                id = id.value,
                spreadsheet_id = sheetSpreadsheetId.spreadsheetId,
                sheet_id = sheetSpreadsheetId.sheetId,
                name = name,
                last_updated_at = lastUpdatedAt!!.epochSecond
            )
        }
    }
}