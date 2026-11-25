package com.hamzaapps.hifzpath.data.quran

/** One surah of the Quran, with its page range in the 604-page Madani mushaf. */
data class Surah(
    val number: Int,
    val transliteration: String,
    val meaning: String,
    val arabic: String,
    val ayahCount: Int,
    val startPage: Int,
    val endPage: Int,
    val meccan: Boolean,
) {
    val pages: IntRange get() = startPage..endPage
}

object Quran {
    const val PAGE_COUNT: Int = TOTAL_PAGES
    const val JUZ_COUNT: Int = 30
    const val AYAH_COUNT: Int = TOTAL_AYAHS

    val surahs: List<Surah> = SURAHS
    val allPages: IntRange = 1..PAGE_COUNT

    private val surahsByPage: Array<List<Surah>> =
        Array(PAGE_COUNT + 1) { page -> surahs.filter { page in it.pages } }

    fun surah(number: Int): Surah = surahs[number - 1]

    fun surahsOnPage(page: Int): List<Surah> =
        if (page in allPages) surahsByPage[page] else emptyList()

    /** The juz that the given page starts in. */
    fun juzOfPage(page: Int): Int {
        var juz = 1
        for (i in JUZ_START_PAGES.indices) {
            if (JUZ_START_PAGES[i] <= page) juz = i + 1
        }
        return juz
    }

    fun pagesOfJuz(juz: Int): IntRange {
        require(juz in 1..JUZ_COUNT) { "juz must be 1..30" }
        val start = JUZ_START_PAGES[juz - 1]
        val end = if (juz == JUZ_COUNT) PAGE_COUNT else JUZ_START_PAGES[juz] - 1
        return start..end
    }
}
