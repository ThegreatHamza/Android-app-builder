package com.hamzaapps.hifzpath

import com.hamzaapps.hifzpath.backup.BackupFile
import com.hamzaapps.hifzpath.backup.BackupFormat
import com.hamzaapps.hifzpath.backup.InvalidBackupException
import com.hamzaapps.hifzpath.backup.ProgressDto
import com.hamzaapps.hifzpath.backup.StudentDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupParseTest {
    private val manager = BackupFormat

    private fun file(pages: List<Int> = listOf(1, 604)) = BackupFile(
        students = listOf(StudentDto(1, "A", 1, 4, 30, 7, 0)),
        progress = pages.map { ProgressDto(1, it, 2, 2) },
        logs = emptyList(),
    )

    @Test fun roundTrip() {
        val text = Json.encodeToString(BackupFile.serializer(), file())
        assertEquals(2, manager.parse(text).progress.size)
    }

    @Test(expected = InvalidBackupException::class)
    fun rejectsGarbage() {
        manager.parse("{not json")
    }

    @Test(expected = InvalidBackupException::class)
    fun rejectsOutOfRangePages() {
        manager.parse(Json.encodeToString(BackupFile.serializer(), file(listOf(700))))
    }
}
