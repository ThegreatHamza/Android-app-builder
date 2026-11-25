package com.hamzaapps.hifzpath

import com.hamzaapps.hifzpath.data.quran.Quran
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranDataTest {
    @Test fun has114SurahsAnd6236Ayahs() {
        assertEquals(114, Quran.surahs.size)
        assertEquals(6236, Quran.surahs.sumOf { it.ayahCount })
        assertEquals(6236, Quran.AYAH_COUNT)
    }

    @Test fun pagesAreContinuousAndCoverMushaf() {
        assertEquals(1, Quran.surahs.first().startPage)
        assertEquals(604, Quran.surahs.last().endPage)
        Quran.surahs.zipWithNext().forEach { (a, b) ->
            assertTrue("${b.number} starts before ${a.number} ends", b.startPage >= a.endPage)
            assertTrue("gap after ${a.number}", b.startPage <= a.endPage + 1)
        }
        (1..604).forEach { assertTrue("page $it has no surah", Quran.surahsOnPage(it).isNotEmpty()) }
    }

    @Test fun juzBoundaries() {
        assertEquals(1, Quran.juzOfPage(1))
        assertEquals(1, Quran.juzOfPage(21))
        assertEquals(2, Quran.juzOfPage(22))
        assertEquals(30, Quran.juzOfPage(604))
        assertEquals(582..604, Quran.pagesOfJuz(30))
        assertEquals(604, (1..30).sumOf { Quran.pagesOfJuz(it).count() })
    }

    @Test fun knownSurahs() {
        val baqarah = Quran.surah(2)
        assertEquals(286, baqarah.ayahCount)
        assertEquals(2, baqarah.startPage)
        assertEquals(49, baqarah.endPage)
        val yasin = Quran.surah(36)
        assertEquals(83, yasin.ayahCount)
        assertEquals(440, yasin.startPage)
        assertEquals(listOf(112, 113, 114), Quran.surahsOnPage(604).map { it.number })
    }
}
