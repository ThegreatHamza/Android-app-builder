package com.hamzaapps.sitecalc.util

import java.text.NumberFormat

object Fmt {
    fun number(value: Double, whole: Boolean = false): String {
        val nf = NumberFormat.getNumberInstance()
        nf.maximumFractionDigits = if (whole) 0 else 2
        nf.minimumFractionDigits = 0
        return nf.format(value)
    }

    fun money(value: Double, currency: String): String {
        val nf = NumberFormat.getNumberInstance()
        nf.maximumFractionDigits = 2
        nf.minimumFractionDigits = 2
        val s = nf.format(value)
        return if (currency.isBlank()) s else "$currency $s"
    }
}
