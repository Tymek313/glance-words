package com.example.words.repository

import com.example.words.datasource.CSVLine
import com.example.words.datasource.GoogleWordsRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Url
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@MockKExtension.ConfirmVerification
class GoogleWordsRemoteDataSourceTest {

    private lateinit var dataSource: GoogleWordsRemoteDataSource
    private lateinit var httpClient: HttpClient
    private lateinit var spreadsheetId: String
    private var sheetId by Delegates.notNull<Int>()
    private lateinit var response: String

    @Before
    fun setUp() {
        spreadsheetId = UUID.randomUUID().toString()
        sheetId = Random.nextInt()
        httpClient = HttpClient(
            MockEngine { request ->
                if (request.url == Url("https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&gid=$sheetId")) {
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

        val csvLines = dataSource.getWords(spreadsheetId, sheetId)

        assertTrue(csvLines.isEmpty())
    }

    @Test
    fun `when words are requested_given response contains words_csv lines are returned`() = runTest {
        response = "a,b\r\nc,d"

        val csvLines = dataSource.getWords(spreadsheetId, sheetId)

        assertEquals(listOf(CSVLine("a,b"), CSVLine("c,d")), csvLines)
    }

    @Test
    fun `when words are requested_given csv contains trailing empty values_they are removed`() = runTest {
        response = "a,b\r\n#VALUE!,#VALUE!\r\n#VALUE!,#VALUE!"

        val csvLines = dataSource.getWords(spreadsheetId, sheetId)

        assertEquals(listOf(CSVLine("a,b")), csvLines)
    }

    @Test
    fun `when words are requested_given csv contains empty values in the middle_they are not removed`() = runTest {
        response = "a,b\r\n#VALUE!,#VALUE!\r\nc,d"

        val csvLines = dataSource.getWords(spreadsheetId, sheetId)

        assertEquals(listOf(CSVLine("a,b"), CSVLine("#VALUE!,#VALUE!"), CSVLine("c,d")), csvLines)
    }
}