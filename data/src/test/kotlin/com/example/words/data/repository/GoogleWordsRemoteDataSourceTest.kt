package com.example.words.data.repository

import com.example.domain.model.SheetSpreadsheetId
import com.example.words.data.datasource.CSVLine
import com.example.words.data.datasource.GoogleWordsRemoteDataSource
import com.example.words.data.fixture.randomSheetSpreadsheetId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoogleWordsRemoteDataSourceTest {

    private lateinit var dataSource: GoogleWordsRemoteDataSource
    private lateinit var httpClient: HttpClient
    private lateinit var sheetSpreadsheetId: SheetSpreadsheetId
    private lateinit var response: String

    @Before
    fun setUp() {
        sheetSpreadsheetId = randomSheetSpreadsheetId()
        httpClient = HttpClient(
            MockEngine { request ->
                if (request.url == Url("https://docs.google.com/spreadsheets/d/${sheetSpreadsheetId.spreadsheetId}/export?format=csv&gid=${sheetSpreadsheetId.sheetId}")) {
                    respond(response)
                } else {
                    error("Unexpected request url")
                }
            }
        )
        dataSource = GoogleWordsRemoteDataSource(httpClient)
    }

    @Test
    fun `when words are requested_given response is empty_empty list is returned`() = runTest {
        response = ""

        val csvLines = dataSource.getWords(sheetSpreadsheetId)

        assertTrue(csvLines.isEmpty())
    }

    @Test
    fun `when words are requested_given response contains words_csv lines are returned`() = runTest {
        response = "a,b\r\nc,d"

        val csvLines = dataSource.getWords(sheetSpreadsheetId)

        assertEquals(listOf(CSVLine("a,b"), CSVLine("c,d")), csvLines)
    }

    @Test
    fun `when words are requested_given csv contains trailing empty values_they are removed`() = runTest {
        response = "a,b\r\n#VALUE!,#VALUE!\r\n#VALUE!,#VALUE!"

        val csvLines = dataSource.getWords(sheetSpreadsheetId)

        assertEquals(listOf(CSVLine("a,b")), csvLines)
    }

    @Test
    fun `when words are requested_given csv contains empty values in the middle_they are not removed`() = runTest {
        response = "a,b\r\n#VALUE!,#VALUE!\r\nc,d"

        val csvLines = dataSource.getWords(sheetSpreadsheetId)

        assertEquals(
            listOf(
                CSVLine("a,b"),
                CSVLine("#VALUE!,#VALUE!"),
                CSVLine("c,d")
            ), csvLines
        )
    }

    @Test
    fun `when words are requested_given csv contains only empty values_no words are returned`() = runTest {
        response = "#VALUE!,#VALUE!"

        val csvLines = dataSource.getWords(sheetSpreadsheetId)

        assertEquals(emptyList(), csvLines)
    }
}