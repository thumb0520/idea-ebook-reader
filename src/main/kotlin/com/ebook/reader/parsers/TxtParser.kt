package com.ebook.reader.parsers

import com.ebook.reader.BookContent
import com.ebook.reader.Chapter
import com.ebook.reader.EbookParser
import java.io.File

class TxtParser : EbookParser {
    override fun parse(file: File): BookContent {
        val content = file.readText()
        val chapters = splitIntoChapters(content)
        return BookContent(
            title = file.nameWithoutExtension,
            chapters = chapters
        )
    }

    override fun supports(fileName: String): Boolean {
        return fileName.endsWith(".txt", ignoreCase = true)
    }

    private fun splitIntoChapters(content: String): List<Chapter> {
        val lines = content.lines()
        val chapters = mutableListOf<Chapter>()
        val currentContent = StringBuilder()
        var currentTitle = "Chapter 1"
        var chapterNum = 1

        for (line in lines) {
            if (isChapterHeader(line)) {
                if (currentContent.isNotEmpty()) {
                    chapters.add(Chapter(currentTitle, currentContent.toString().trim()))
                    currentContent.clear()
                }
                chapterNum++
                currentTitle = line.trim()
            } else {
                currentContent.appendLine(line)
            }
        }

        if (currentContent.isNotEmpty()) {
            chapters.add(Chapter(currentTitle, currentContent.toString().trim()))
        }

        return chapters.ifEmpty {
            listOf(Chapter("Content", content))
        }
    }

    private fun isChapterHeader(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.matches(Regex("^(第[一二三四五六七八九十百千零\\d]+[章节回卷]|Chapter\\s+\\d+).*"))
    }
}
