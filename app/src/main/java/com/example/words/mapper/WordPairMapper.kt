package com.example.words.mapper

import com.example.words.datasource.CSVLine
import com.example.words.model.WordPair

interface WordPairMapper {
    fun map(line: CSVLine): WordPair
}

class DefaultWordPairMapper: WordPairMapper {
    override fun map(line: CSVLine): WordPair = line.value
        .split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
        .run { WordPair(get(0), getOrNull(1).orEmpty()) }
}