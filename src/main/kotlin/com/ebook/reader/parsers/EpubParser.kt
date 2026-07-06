package com.ebook.reader.parsers

import com.ebook.reader.BookContent
import com.ebook.reader.Chapter
import com.ebook.reader.EbookParser
import io.documentnode.epub4j.epub.EpubReader
import org.jsoup.Jsoup
import java.io.File

class EpubParser : EbookParser {
    override fun parse(file: File): BookContent {
        val epubReader = EpubReader()
        val book = epubReader.readEpub(file.inputStream())

        val title = book.title ?: file.nameWithoutExtension
        val chapters = mutableListOf<Chapter>()

        book.contents.forEachIndexed { index, resource ->
            val html = String(resource.data)
            val text = extractText(html)
            val chapterTitle = extractTitle(html) ?: "Chapter ${index + 1}"
            if (text.isNotBlank()) {
                chapters.add(Chapter(chapterTitle, text))
            }
        }

        return BookContent(title, chapters)
    }

    override fun supports(fileName: String): Boolean {
        return fileName.endsWith(".epub", ignoreCase = true)
    }

    private fun extractText(html: String): String {
        val doc = Jsoup.parse(html)
        return doc.body()?.text() ?: ""
    }

    private fun extractTitle(html: String): String? {
        val doc = Jsoup.parse(html)
        return doc.selectFirst("h1, h2, h3, title")?.text()
    }
}
