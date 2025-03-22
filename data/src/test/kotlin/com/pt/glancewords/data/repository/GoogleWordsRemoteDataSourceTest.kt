package com.pt.glancewords.data.repository

import com.pt.glancewords.data.CSVWordPairMapper
import com.pt.glancewords.data.datasource.GoogleWordsRemoteDataSource
import com.pt.glancewords.data.fixture.randomSheetSpreadsheetId
import com.pt.glancewords.domain.model.SheetSpreadsheetId
import com.pt.glancewords.domain.model.WordPair
import com.pt.testcommon.fixture.randomString
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.Url
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GoogleWordsRemoteDataSourceTest {

    private lateinit var dataSource: GoogleWordsRemoteDataSource
    private lateinit var fakeHttpClient: HttpClient
    private lateinit var fakeMapper: CSVWordPairMapper
    private lateinit var sheetSpreadsheetId: SheetSpreadsheetId
    private lateinit var response: String
    private var requestFails = false

    private val everyMap get() = every { fakeMapper.map(CSV_WITH_WORDS_RESPONSE) }

    private fun everyMapReturnsWordPair() = everyMap returns MAPPED_WORD_PAIRS

    @Before
    fun setUp() {
        sheetSpreadsheetId = randomSheetSpreadsheetId()
        requestFails = false
        fakeHttpClient = HttpClient(
            MockEngine { request ->
                when {
                    requestFails -> throw IOException("network error")
                    request.url == Url("https://docs.google.com/spreadsheets/d/${sheetSpreadsheetId.spreadsheetId}/export?format=csv&gid=${sheetSpreadsheetId.sheetId}") -> {
                        respondOk(response)
                    }

                    else -> error("Unexpected request url")
                }
            }
        )
        fakeMapper = mockk()
        dataSource = GoogleWordsRemoteDataSource(
            client = fakeHttpClient,
            logger = mockk(relaxed = true),
            mapper = fakeMapper
        )
    }

    @Test
    fun `when words are requested_given request fails_then null is returned`() = runTest {
        requestFails = true

        val result = dataSource.getWords(sheetSpreadsheetId)

        assertNull(result)
    }

    @Test
    fun `when words are requested_given response contains word pairs_then word pairs are returned`() = runTest {
        response = CSV_WITH_WORDS_RESPONSE
        everyMapReturnsWordPair()

        val result = dataSource.getWords(sheetSpreadsheetId)

        assertEquals(MAPPED_WORD_PAIRS, result)
    }

    private companion object {
        val CSV_WITH_WORDS_RESPONSE = randomString()
        val MAPPED_WORD_PAIRS = listOf(WordPair(original = randomString(), translated = randomString()))
    }
}