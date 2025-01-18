package com.example.words.data.mapper

import com.example.domain.model.WordPair
import com.example.words.data.datasource.CSVLine
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultWordPairMapperTest {

    private lateinit var mapper: DefaultWordPairMapper

    @Before
    fun setUp() {
        mapper = DefaultWordPairMapper()
    }

    @Test
    fun `when csv line is mapped_given there are two columns_both words are mapped`() {
        val wordPair = mapper.map(CSVLine("a,b"))

        assertEquals(WordPair("a", "b"), wordPair)
    }

    @Test
    fun `when csv line is mapped_given there are more than 2 columns_only two first words are mapped`() {
        val wordPair = mapper.map(CSVLine("a,b,c"))

        assertEquals(WordPair("a", "b"), wordPair)
    }

    @Test
    fun `when csv line is mapped_given there is first column_first word is mapped and second is empty`() {
        val wordPair = mapper.map(CSVLine("a,"))

        assertEquals(WordPair("a", ""), wordPair)
    }

    @Test
    fun `when csv line is mapped_given there is second column_first word is empty and second is mapped`() {
        val wordPair = mapper.map(CSVLine(",b"))

        assertEquals(WordPair("", "b"), wordPair)
    }

    @Test
    fun `when csv line is mapped_given there are no columns_both words are empty`() {
        val wordPair = mapper.map(CSVLine(","))

        assertEquals(WordPair("", ""), wordPair)
    }

    @Test
    fun `when csv line is mapped_given there are no columns without a separator_both words are empty`() {
        val wordPair = mapper.map(CSVLine(""))

        assertEquals(WordPair("", ""), wordPair)
    }
}