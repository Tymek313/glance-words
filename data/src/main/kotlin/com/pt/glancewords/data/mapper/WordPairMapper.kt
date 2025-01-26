package com.pt.glancewords.data.mapper

import com.pt.glancewords.data.datasource.CSVLine
import com.pt.glancewords.domain.model.WordPair

internal interface WordPairMapper {
    fun map(line: CSVLine): WordPair
}

internal class DefaultWordPairMapper : WordPairMapper {
    override fun map(line: CSVLine): WordPair = line.value
        .split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
        .run { WordPair(get(0), getOrNull(1).orEmpty()) }
}