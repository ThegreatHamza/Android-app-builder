package com.hamzaapps.sitecalc.backup

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** A project report ready to export. All text is already localized. */
data class Report(
    val title: String,
    val subtitle: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val totalLabel: String,
    val total: String,
    val footer: String,
)

object ReportWriter {

    fun csv(report: Report): String = buildString {
        append(report.headers.joinToString(",") { escape(it) }).append("\r\n")
        report.rows.forEach { row -> append(row.joinToString(",") { escape(it) }).append("\r\n") }
        append(listOf(report.totalLabel, "", "", report.total).joinToString(",") { escape(it) }).append("\r\n")
    }

    internal fun escape(value: String): String {
        // Guard against spreadsheet formula injection, then quote.
        val safe = if (value.firstOrNull() in setOf('=', '+', '-', '@')) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    /** Writes a simple A4 PDF. */
    fun pdf(report: Report, out: OutputStream) {
        val doc = PdfDocument()
        val width = 595
        val height = 842
        val margin = 40f
        val title = Paint().apply { textSize = 20f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val bold = Paint().apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { textSize = 10f; isAntiAlias = true }
        val small = Paint().apply { textSize = 8f; isAntiAlias = true; color = 0xFF666666.toInt() }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
            y = margin + 10f
        }

        fun line(text: String, paint: Paint, x: Float = margin, advance: Float = paint.textSize + 6f) {
            if (y + advance > height - margin) newPage()
            page!!.canvas.drawText(text, x, y, paint)
            y += advance
        }

        newPage()
        line(report.title, title, advance = 28f)
        line(report.subtitle, small, advance = 20f)
        report.rows.forEachIndexed { index, row ->
            // row: [title, calculator, details, cost]
            line("${index + 1}. ${row[0]}  (${row[1]})", bold)
            wrap(row[2], body, width - 2 * margin - 10).forEach { line(it, body, margin + 10) }
            if (row[3].isNotBlank()) line(row[3], body, margin + 10)
            y += 6f
        }
        y += 6f
        line("${report.totalLabel}: ${report.total}", bold)
        y += 10f
        wrap(report.footer, small, width - 2 * margin).forEach { line(it, small) }
        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.split("\n").forEach { paragraph ->
            var current = ""
            paragraph.split(" ").forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) result += current
                    current = word
                }
            }
            result += current
        }
        return result
    }
}
