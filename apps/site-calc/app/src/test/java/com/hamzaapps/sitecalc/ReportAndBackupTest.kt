package com.hamzaapps.sitecalc

import com.hamzaapps.sitecalc.backup.BackupFile
import com.hamzaapps.sitecalc.backup.BackupFormat
import com.hamzaapps.sitecalc.backup.InvalidBackupException
import com.hamzaapps.sitecalc.backup.ItemDto
import com.hamzaapps.sitecalc.backup.ProjectDto
import com.hamzaapps.sitecalc.backup.Report
import com.hamzaapps.sitecalc.backup.ReportWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAndBackupTest {
    @Test fun csvEscapesQuotesAndFormulas() {
        assertEquals("\"a \"\"b\"\"\"", ReportWriter.escape("a \"b\""))
        assertEquals("\"'=SUM(A1)\"", ReportWriter.escape("=SUM(A1)"))
        val csv = ReportWriter.csv(Report("P", "s", listOf("Item", "Calc", "Qty", "Cost"), listOf(listOf("Slab", "Concrete", "Cement: 20 bags", "200")), "Total", "200", "f"))
        assertTrue(csv.startsWith("\"Item\",\"Calc\""))
        assertEquals(3, csv.trim().lines().size)
    }

    @Test fun backupRoundTripAndValidation() {
        val file = BackupFile(
            projects = listOf(ProjectDto(1, "House", 0)),
            items = listOf(ItemDto(1, "CONCRETE", "Slab", "{}", "{}", "", 0.0, 0)),
        )
        val text = BackupFormat.json.encodeToString(BackupFile.serializer(), file)
        assertEquals(1, BackupFormat.parse(text).items.size)
    }

    @Test(expected = InvalidBackupException::class)
    fun backupRejectsUnknownCalculator() {
        val file = BackupFile(projects = listOf(ProjectDto(1, "H", 0)), items = listOf(ItemDto(1, "LASER", "x", "{}", "{}", "", 0.0, 0)))
        BackupFormat.parse(BackupFormat.json.encodeToString(BackupFile.serializer(), file))
    }
}
