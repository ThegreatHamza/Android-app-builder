package com.hamzaapps.sitecalc

import com.hamzaapps.sitecalc.domain.CalcResult
import com.hamzaapps.sitecalc.domain.CalcType
import com.hamzaapps.sitecalc.domain.Calculators
import com.hamzaapps.sitecalc.domain.ConcreteShape
import com.hamzaapps.sitecalc.domain.InvalidInputException
import com.hamzaapps.sitecalc.domain.Material
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class CalculatorsTest {
    private fun calc(type: CalcType, inputs: Map<String, String>, options: Map<String, Double> = emptyMap(), prices: Map<Material, Double> = emptyMap()) =
        Calculators.calculate(type, inputs, options, bagKg = 50.0, prices = prices)

    private fun CalcResult.value(label: Int) = lines.first { it.label == label }.shownValue

    @Test fun concreteSlab() {
        val r = calc(
            CalcType.CONCRETE,
            mapOf("length" to "5", "width" to "4", "thickness" to "150", "waste" to "0"),
            mapOf("shape" to ConcreteShape.SLAB, "mix" to 7.0),
            mapOf(Material.CEMENT_BAG to 10.0),
        )
        assertEquals(3.0, r.value(R.string.r_volume), 1e-9)
        assertEquals(20.0, r.value(R.string.r_cement), 1e-9) // 19.008 bags rounded up
        assertEquals(1.32, r.value(R.string.r_sand), 1e-9)
        assertEquals(2.64, r.value(R.string.r_aggregate), 1e-9)
        assertEquals(475.2, r.value(R.string.r_water), 1e-6)
        assertEquals(200.0, r.cost, 1e-9)
    }

    @Test fun roundColumnUsesDiameter() {
        val r = calc(
            CalcType.CONCRETE,
            mapOf("diameter" to "300", "height" to "3", "count" to "2", "waste" to "0"),
            mapOf("shape" to ConcreteShape.ROUND_COLUMN),
        )
        assertEquals(Math.PI * 0.15 * 0.15 * 3 * 2, r.value(R.string.r_volume), 1e-9)
    }

    @Test fun blocksWall() {
        val r = calc(
            CalcType.BLOCKS,
            mapOf("length" to "10", "height" to "3", "openings" to "0", "blockL" to "400", "blockH" to "200", "blockT" to "150", "joint" to "10", "waste" to "0"),
        )
        assertEquals(30.0, r.value(R.string.r_wall_area), 1e-9)
        assertEquals(349.0, r.value(R.string.r_blocks), 1e-9) // 348.43 rounded up
    }

    @Test fun openingsLargerThanWallIsInvalid() {
        try {
            calc(CalcType.BLOCKS, mapOf("length" to "2", "height" to "2", "openings" to "5", "blockL" to "400", "blockH" to "200", "blockT" to "150", "joint" to "10", "waste" to "0"))
            fail("expected invalid input")
        } catch (e: InvalidInputException) {
            assertEquals("openings", e.fieldKey)
        }
    }

    @Test fun tiles() {
        val r = calc(CalcType.TILES, mapOf("length" to "4", "width" to "3", "tileL" to "600", "tileW" to "600", "perBox" to "4", "waste" to "10"))
        assertEquals(37.0, r.value(R.string.r_tiles), 1e-9)
        assertEquals(10.0, r.value(R.string.r_tile_boxes), 1e-9)
    }

    @Test fun paint() {
        val r = calc(CalcType.PAINT, mapOf("area" to "50", "openings" to "5", "coverage" to "10", "can" to "4"), mapOf("coats" to 2.0))
        assertEquals(9.0, r.value(R.string.r_paint), 1e-9)
        assertEquals(3.0, r.value(R.string.r_cans), 1e-9)
    }

    @Test fun rebar() {
        val r = calc(CalcType.REBAR, mapOf("length" to "6", "count" to "10", "stock" to "12"), mapOf("diameter" to 12.0))
        assertEquals(53.333, r.value(R.string.r_steel_weight), 1e-3)
        assertEquals(5.0, r.value(R.string.r_stock_bars), 1e-9)
    }

    @Test fun excavation() {
        val r = calc(CalcType.EXCAVATION, mapOf("length" to "10", "width" to "1", "depth" to "1", "swell" to "25", "truck" to "10"))
        assertEquals(12.5, r.value(R.string.r_loose_volume), 1e-9)
        assertEquals(2.0, r.value(R.string.r_trucks), 1e-9)
    }

    @Test fun roofingGable() {
        val r = calc(CalcType.ROOFING, mapOf("length" to "10", "rafter" to "4", "cover" to "800", "sheetLen" to "3", "lap" to "150"), mapOf("sides" to 2.0))
        assertEquals(13.0, r.value(R.string.r_sheets_across), 1e-9)
        assertEquals(2.0, r.value(R.string.r_sheet_rows), 1e-9)
        assertEquals(52.0, r.value(R.string.r_sheets), 1e-9)
    }

    @Test fun parsingAcceptsCommaAndRejectsGarbage() {
        assertEquals(1.5, Calculators.parseNumber(" 1,5 ")!!, 1e-9)
        assertNull(Calculators.parseNumber("abc"))
        assertNull(Calculators.parseNumber(""))
        try {
            calc(CalcType.EXCAVATION, mapOf("length" to "-1", "width" to "1", "depth" to "1", "swell" to "25", "truck" to "10"))
            fail("negative length must be rejected")
        } catch (e: InvalidInputException) {
            assertEquals("length", e.fieldKey)
        }
    }

    @Test fun everyCalculatorWorksWithDefaultsPlusRequiredValues() {
        CalcType.entries.forEach { type ->
            val spec = Calculators.spec(type)
            val inputs = spec.fields(Calculators.defaultOptions(type)).associate { it.key to (it.default?.toString() ?: "3") }
            val result = calc(type, inputs)
            assert(result.lines.isNotEmpty()) { "$type has no results" }
            assert(result.lines.all { it.value.isFinite() && it.value >= 0 }) { "$type produced a bad number" }
        }
    }
}
