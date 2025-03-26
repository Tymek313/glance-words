package com.pt.glancewords.data.words.mapper

import com.pt.glancewords.domain.words.model.WordPair

interface CsvWordPairMapper {
    fun map(csv: String): List<WordPair>
}

class DefaultCsvWordPairMapper : CsvWordPairMapper {

    override fun map(csv: String): List<WordPair> = csv.split("\r\n").mapNotNull { rows ->
        val cells = rows.split(COMMA_SEPARATOR_REGEX).take(2)
        if (cells.all { it == "#VALUE!" || it.isBlank() }) {
            null
        } else {
            WordPair(cells[0].trim('"'), cells[1].trim('"'))
        }
    }

    companion object {
        private val COMMA_SEPARATOR_REGEX = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex()
    }
}
