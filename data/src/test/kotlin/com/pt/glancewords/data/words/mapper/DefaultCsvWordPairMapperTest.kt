package com.pt.glancewords.data.words.mapper

import com.pt.glancewords.domain.words.model.WordPair
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DefaultCsvWordPairMapperTest {

    private lateinit var mapper: DefaultCsvWordPairMapper

    @Before
    fun setUp() {
        mapper = DefaultCsvWordPairMapper()
    }

    @Test
    fun `when mapping_given csv contains two columns and multiple rows_then they are mapped into word pairs`() = runTest {
        val result = mapper.map("a,b\r\nc,d")

        assertEquals(
            listOf(
                WordPair(original = "a", translated = "b"),
                WordPair(original = "c", translated = "d"),
            ),
            result
        )
    }

    @Test
    fun `when mapping_given all cells in a row contain error values_then the row is removed`() = runTest {
        val result = mapper.map("a,b\r\n#VALUE!,#VALUE!\r\nc,d\r\n#VALUE!,#VALUE!")

        assertEquals(
            listOf(
                WordPair(original = "a", translated = "b"),
                WordPair(original = "c", translated = "d"),
            ),
            result
        )
    }

    @Test
    fun `when mapping_given all cells in a row contain empty values_then the row is removed`() = runTest {
        val result = mapper.map("a,b\r\n,\r\nc,d\r\n")

        assertEquals(
            listOf(
                WordPair(original = "a", translated = "b"),
                WordPair(original = "c", translated = "d"),
            ),
            result
        )
    }

    @Test
    fun `when mapping_given one of the cells in a row contains an error value_then row is preserved`() = runTest {
        val result = mapper.map("a,b\r\n#VALUE!,d\r\ne,f\r\ng,#VALUE!")

        assertEquals(
            listOf(
                WordPair(original = "a", translated = "b"),
                WordPair(original = "#VALUE!", translated = "d"),
                WordPair(original = "e", translated = "f"),
                WordPair(original = "g", translated = "#VALUE!"),
            ),
            result
        )
    }

    @Test
    fun `when mapping_given a row contains more than two columns_then only first two columns are used`() = runTest {
        val result = mapper.map("a,b,c,d")

        assertEquals(
            listOf(WordPair(original = "a" , translated = "b")),
            result
        )
    }

    @Test
    fun `when mapping_given csv cell is wrapped with double quotes_then quotes are removed`() = runTest {
        val result = mapper.map("\"a\",\"b\"")

        assertEquals(
            listOf(WordPair(original = "a" , translated = "b")),
            result
        )
    }

    @Test
    fun `when mapping_given csv cell wrapped with double quotes contains a comma_then it is not counted as a csv separator`() = runTest {
        val result = mapper.map("\"a,b\",b")

        assertEquals(
            listOf(WordPair(original = "a,b" , translated = "b")),
            result
        )
    }
}