package com.example.words.data.mapper

import com.example.domain.model.WordPair
import com.example.words.data.datasource.CSVLine

internal interface WordPairMapper {
    fun map(line: CSVLine): WordPair
}

internal class DefaultWordPairMapper : WordPairMapper {
    override fun map(line: CSVLine): WordPair = line.value
        .split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
        .run { WordPair(get(0), getOrNull(1).orEmpty()) }
}