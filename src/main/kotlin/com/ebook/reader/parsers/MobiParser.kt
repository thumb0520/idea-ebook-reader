package com.ebook.reader.parsers

import com.ebook.reader.BookContent
import com.ebook.reader.Chapter
import com.ebook.reader.EbookParser
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class MobiParser : EbookParser {
    override fun parse(file: File): BookContent {
        val content = parseMobiFile(file)
        val chapters = listOf(Chapter("Content", content))
        return BookContent(file.nameWithoutExtension, chapters)
    }

    override fun supports(fileName: String): Boolean {
        return fileName.endsWith(".mobi", ignoreCase = true)
    }

    private fun parseMobiFile(file: File): String {
        return try {
            val bytes = file.readBytes()
            val header = parsePalmDocHeader(bytes)

            if (header.compression == 2) {
                decompressPalmDoc(bytes, header)
            } else {
                extractRawText(bytes, header)
            }
        } catch (e: Exception) {
            "Unable to parse MOBI file: ${e.message}"
        }
    }

    private data class PalmDocHeader(
        val compression: Int,
        val textLength: Int,
        val recordCount: Int,
        val recordSize: Int
    )

    private fun parsePalmDocHeader(bytes: ByteArray): PalmDocHeader {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(0)
        // Skip PDB name (32 bytes)
        buffer.position(32)
        // Skip attributes, version, dates, etc.
        buffer.position(44)
        val recordCount = buffer.short.toInt() and 0xFFFF

        // First record offset
        val firstRecordOffset = if (recordCount > 0) {
            buffer.position(76)
            buffer.int
        } else {
            78
        }

        // PalmDOC header at first record
        buffer.position(firstRecordOffset)
        val compression = buffer.short.toInt() and 0xFFFF
        buffer.short // padding
        val textLength = buffer.int

        return PalmDocHeader(
            compression = compression,
            textLength = textLength,
            recordCount = recordCount,
            recordSize = 4096
        )
    }

    private fun decompressPalmDoc(bytes: ByteArray, header: PalmDocHeader): String {
        val result = StringBuilder()
        var pos = 78 + 16 // Skip PDB header and PalmDOC header
        val end = minOf(pos + header.textLength, bytes.size)

        while (pos < end) {
            val byte = bytes[pos].toInt() and 0xFF
            when {
                byte == 0 -> {
                    result.append("\n")
                    pos++
                }
                byte in 0x01..0x08 -> {
                    for (i in 0 until byte) {
                        if (pos + 1 < end) {
                            result.append((bytes[pos + 1].toInt() and 0xFF).toChar())
                        }
                        pos++
                    }
                    pos++
                }
                byte in 0x80..0xBF -> {
                    if (pos + 1 < end) {
                        val next = bytes[pos + 1].toInt() and 0xFF
                        val distance = ((byte and 0x3F) shl 8) or next
                        val length = ((byte and 0xC0) shr 6) + 3
                        val copyPos = result.length - distance
                        for (i in 0 until length) {
                            if (copyPos + i in 0 until result.length) {
                                result.append(result[copyPos + i])
                            }
                        }
                    }
                    pos += 2
                }
                byte >= 0xC0 -> {
                    result.append(" ")
                    result.append((byte xor 0x80).toChar())
                    pos++
                }
                else -> {
                    result.append(byte.toChar())
                    pos++
                }
            }
        }

        return cleanText(result.toString())
    }

    private fun extractRawText(bytes: ByteArray, header: PalmDocHeader): String {
        val start = 78 + 16
        val end = minOf(start + header.textLength, bytes.size)
        val textBytes = bytes.copyOfRange(start, end)
        return cleanText(String(textBytes, StandardCharsets.UTF_8))
    }

    private fun cleanText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\\x00"), "")
            .trim()
    }
}
