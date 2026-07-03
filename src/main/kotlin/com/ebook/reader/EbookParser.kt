package com.ebook.reader

import java.io.File

data class BookContent(
    val title: String,
    val chapters: List<Chapter>
)

data class Chapter(
    val title: String,
    val content: String
)

interface EbookParser {
    fun parse(file: File): BookContent
    fun supports(fileName: String): Boolean
}

object ParserFactory {
    private val parsers = listOf(TxtParser(), EpubParser(), MobiParser())

    fun getParser(fileName: String): EbookParser? {
        return parsers.find { it.supports(fileName) }
    }
}
