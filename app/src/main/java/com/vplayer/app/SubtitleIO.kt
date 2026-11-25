package com.vplayer.app

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale

object SubtitleIO {

    val EXTENSIONS = setOf("srt", "ass", "ssa", "vtt")

    private val timeRe = Regex("""(?:(\d+):)?(\d{1,2}):(\d{2})[.,](\d{1,3})""")

    /** Decodes bytes: BOM aware, strict UTF-8 first, then Windows-1256 (Persian/Arabic). */
    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1256"))
        }
    }

    fun parse(raw: String): List<SubCue> {
        val t = raw.replace("\r\n", "\n").replace('\r', '\n')
        val isAss = t.contains("[Script Info]", ignoreCase = true) ||
            t.contains("\nDialogue:") || t.startsWith("Dialogue:")
        return if (isAss) parseAss(t) else parseSrtVtt(t)
    }

    fun parseTime(s: String): Long? {
        val m = timeRe.find(s.trim()) ?: return null
        val h = m.groupValues[1].ifEmpty { "0" }.toLong()
        val mi = m.groupValues[2].toLong()
        val sec = m.groupValues[3].toLong()
        val ms = m.groupValues[4].padEnd(3, '0').toLong()
        return ((h * 60 + mi) * 60 + sec) * 1000 + ms
    }

    fun fmt(msIn: Long): String {
        val ms = if (msIn < 0) 0L else msIn
        val h = ms / 3600000
        val m = (ms / 60000) % 60
        val s = (ms / 1000) % 60
        val r = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s, r)
    }

    private fun clean(s: String): String {
        return s
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\{\\\\[^}]*\\}"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()
    }

    private fun parseSrtVtt(t: String): List<SubCue> {
        val cues = ArrayList<SubCue>()
        val lines = t.split("\n")
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.contains("-->")) {
                val parts = line.split("-->")
                val start = parseTime(parts[0])
                val endToken = parts[1].trim().split(Regex("\\s+"))[0]
                val end = parseTime(endToken)
                i++
                val sb = StringBuilder()
                while (i < lines.size && lines[i].isNotBlank()) {
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(lines[i].trim())
                    i++
                }
                val text = clean(sb.toString())
                if (start != null && end != null && text.isNotEmpty()) {
                    cues.add(SubCue(start, end, text))
                }
            } else {
                i++
            }
        }
        return cues.sortedBy { it.startMs }
    }

    private fun parseAss(t: String): List<SubCue> {
        var startIdx = 1
        var endIdx = 2
        var textIdx = 9
        var fieldCount = 10
        val cues = ArrayList<SubCue>()
        for (raw in t.split("\n")) {
            val line = raw.trim()
            if (line.startsWith("Format:", ignoreCase = true) &&
                line.contains("Start", ignoreCase = true) &&
                line.contains("Text", ignoreCase = true)
            ) {
                val f = line.substringAfter(":").split(",").map { it.trim().lowercase() }
                val s = f.indexOf("start")
                val e = f.indexOf("end")
                val x = f.indexOf("text")
                if (s >= 0 && e >= 0 && x >= 0) {
                    startIdx = s
                    endIdx = e
                    textIdx = x
                    fieldCount = f.size
                }
            } else if (line.startsWith("Dialogue:", ignoreCase = true)) {
                val body = line.substringAfter(":").trimStart()
                val p = body.split(",", limit = fieldCount)
                if (p.size < fieldCount) continue
                val s = parseTime(p[startIdx])
                val e = parseTime(p[endIdx])
                val text = clean(
                    p[textIdx].replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ")
                )
                if (s != null && e != null && text.isNotEmpty()) {
                    cues.add(SubCue(s, e, text))
                }
            }
        }
        return cues.sortedBy { it.startMs }
    }

    fun toSrt(cues: List<SubCue>): String {
        val sb = StringBuilder()
        cues.sortedBy { it.startMs }.forEachIndexed { idx, c ->
            sb.append(idx + 1).append('\n')
            sb.append(fmt(c.startMs)).append(" --> ").append(fmt(c.endMs)).append('\n')
            sb.append(c.text).append("\n\n")
        }
        return sb.toString()
    }

    /** UTF-8 with BOM so Persian text shows correctly in most players. */
    fun toBytes(cues: List<SubCue>): ByteArray {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        return bom + toSrt(cues).toByteArray(Charsets.UTF_8)
    }

    /** Subtitle files sitting next to the video (needs file access to see them). */
    fun findSiblings(videoPath: String?): List<File> {
        if (videoPath.isNullOrEmpty()) return emptyList()
        val video = File(videoPath)
        val dir = video.parentFile ?: return emptyList()
        val base = video.nameWithoutExtension
        val files = (try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        }) ?: return emptyList()
        return files
            .filter { it.isFile && it.extension.lowercase() in EXTENSIONS && it.name.startsWith(base) }
            .sortedBy { it.name }
    }
}
