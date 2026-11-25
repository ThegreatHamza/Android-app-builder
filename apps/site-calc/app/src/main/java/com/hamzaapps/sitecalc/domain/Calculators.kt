package com.hamzaapps.sitecalc.domain

import androidx.annotation.StringRes
import com.hamzaapps.sitecalc.R
import kotlin.math.PI
import kotlin.math.ceil

/** Units of measure shown next to inputs and results. */
enum class Uom(@param:StringRes val label: Int) {
    M(R.string.unit_m), MM(R.string.unit_mm), M2(R.string.unit_m2), M3(R.string.unit_m3),
    PERCENT(R.string.unit_percent), KG(R.string.unit_kg), BAGS(R.string.unit_bags), PCS(R.string.unit_pcs),
    LITRES(R.string.unit_litres), BOXES(R.string.unit_boxes), CANS(R.string.unit_cans), TRUCKS(R.string.unit_trucks),
    SHEETS(R.string.unit_sheets), BARS(R.string.unit_bars), COUNT(R.string.unit_count), M2_PER_L(R.string.unit_m2_per_l),
    NONE(R.string.unit_none),
}

/** Materials that can have a unit price in the price book. */
enum class Material(@param:StringRes val label: Int, val unit: Uom) {
    CEMENT_BAG(R.string.mat_cement_bag, Uom.BAGS),
    SAND_M3(R.string.mat_sand, Uom.M3),
    AGGREGATE_M3(R.string.mat_aggregate, Uom.M3),
    BLOCK(R.string.mat_block, Uom.PCS),
    TILE_BOX(R.string.mat_tile_box, Uom.BOXES),
    PAINT_CAN(R.string.mat_paint_can, Uom.CANS),
    STEEL_KG(R.string.mat_steel, Uom.KG),
    TRUCK(R.string.mat_truck, Uom.TRUCKS),
    SHEET(R.string.mat_sheet, Uom.SHEETS),
}

data class InputField(
    val key: String,
    @param:StringRes val label: Int,
    val unit: Uom,
    val default: Double?,
    /** If false the field may be zero (e.g. openings). */
    val mustBePositive: Boolean = true,
)

data class Choice(@param:StringRes val label: Int, val value: Double)

data class OptionField(
    val key: String,
    @param:StringRes val label: Int,
    val choices: List<Choice>,
    val defaultIndex: Int = 0,
)

data class ResultLine(
    @param:StringRes val label: Int,
    val value: Double,
    val unit: Uom,
    /** Whole items (bags, blocks...) are rounded up. */
    val whole: Boolean = false,
    val material: Material? = null,
    /** Key lines are highlighted and used in summaries. */
    val key: Boolean = false,
) {
    val shownValue: Double get() = if (whole) ceil(value - 1e-9) else value
}

data class CalcResult(val lines: List<ResultLine>, val cost: Double) {
    fun costLines(prices: Map<Material, Double>): List<Pair<ResultLine, Double>> =
        lines.filter { it.material != null && (prices[it.material] ?: 0.0) > 0.0 }
            .map { it to it.shownValue * (prices[it.material] ?: 0.0) }
}

class InvalidInputException(val fieldKey: String) : Exception("Invalid value for $fieldKey")

/** Shared constants, documented in DESIGN.md. */
object Factors {
    const val CONCRETE_DRY = 1.54   // dry volume of materials per m³ of wet concrete
    const val MORTAR_DRY = 1.33     // dry volume per m³ of wet mortar
    const val PLASTER_DRY = 1.27    // dry volume per m³ of wet plaster (includes joint filling)
    const val CEMENT_DENSITY = 1440.0 // kg per m³ of loose cement
    const val WATER_CEMENT = 0.5    // litres of water per kg of cement
}

enum class CalcType(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
) {
    CONCRETE(R.string.calc_concrete, R.string.calc_concrete_desc),
    BLOCKS(R.string.calc_blocks, R.string.calc_blocks_desc),
    PLASTER(R.string.calc_plaster, R.string.calc_plaster_desc),
    TILES(R.string.calc_tiles, R.string.calc_tiles_desc),
    PAINT(R.string.calc_paint, R.string.calc_paint_desc),
    REBAR(R.string.calc_rebar, R.string.calc_rebar_desc),
    EXCAVATION(R.string.calc_excavation, R.string.calc_excavation_desc),
    ROOFING(R.string.calc_roofing, R.string.calc_roofing_desc),
}

/** Concrete shapes; the option value selects which dimension fields are used. */
object ConcreteShape {
    const val SLAB = 0.0
    const val RECT_COLUMN = 1.0
    const val ROUND_COLUMN = 2.0
}

data class CalculatorSpec(
    val type: CalcType,
    val options: List<OptionField>,
    /** Fields shown for the given option values. */
    val fields: (Map<String, Double>) -> List<InputField>,
    val compute: (inputs: Map<String, Double>, options: Map<String, Double>, bagKg: Double) -> List<ResultLine>,
)

object Calculators {

    private val mixChoicesConcrete = listOf(
        Choice(R.string.mix_c20, 1 + 1.5 + 3.0),
        Choice(R.string.mix_c15, 1 + 2.0 + 4.0),
        Choice(R.string.mix_c10, 1 + 3.0 + 6.0),
    )

    /** Sand and aggregate parts for each concrete mix, matching [mixChoicesConcrete]. */
    private fun concreteParts(total: Double): Pair<Double, Double> = when (total) {
        5.5 -> 1.5 to 3.0
        7.0 -> 2.0 to 4.0
        else -> 3.0 to 6.0
    }

    private val mortarChoices = listOf(
        Choice(R.string.mix_1_4, 4.0),
        Choice(R.string.mix_1_5, 5.0),
        Choice(R.string.mix_1_6, 6.0),
    )

    private val waste = InputField("waste", R.string.f_waste, Uom.PERCENT, 5.0, mustBePositive = false)

    private fun cementSand(dry: Double, sandParts: Double, bagKg: Double): List<ResultLine> {
        val cementM3 = dry / (1 + sandParts)
        val cementKg = cementM3 * Factors.CEMENT_DENSITY
        return listOf(
            ResultLine(R.string.r_cement, cementKg / bagKg, Uom.BAGS, whole = true, material = Material.CEMENT_BAG, key = true),
            ResultLine(R.string.r_sand, dry * sandParts / (1 + sandParts), Uom.M3, material = Material.SAND_M3, key = true),
        )
    }

    val all: Map<CalcType, CalculatorSpec> = listOf(
        CalculatorSpec(
            type = CalcType.CONCRETE,
            options = listOf(
                OptionField(
                    "shape", R.string.o_shape,
                    listOf(
                        Choice(R.string.shape_slab, ConcreteShape.SLAB),
                        Choice(R.string.shape_rect_column, ConcreteShape.RECT_COLUMN),
                        Choice(R.string.shape_round_column, ConcreteShape.ROUND_COLUMN),
                    ),
                ),
                OptionField("mix", R.string.o_mix, mixChoicesConcrete, defaultIndex = 1),
            ),
            fields = { o ->
                when (o["shape"]) {
                    ConcreteShape.RECT_COLUMN -> listOf(
                        InputField("width", R.string.f_width, Uom.MM, 230.0),
                        InputField("depth", R.string.f_depth, Uom.MM, 230.0),
                        InputField("height", R.string.f_height, Uom.M, 3.0),
                        InputField("count", R.string.f_count, Uom.COUNT, 1.0),
                        waste,
                    )
                    ConcreteShape.ROUND_COLUMN -> listOf(
                        InputField("diameter", R.string.f_diameter, Uom.MM, 300.0),
                        InputField("height", R.string.f_height, Uom.M, 3.0),
                        InputField("count", R.string.f_count, Uom.COUNT, 1.0),
                        waste,
                    )
                    else -> listOf(
                        InputField("length", R.string.f_length, Uom.M, null),
                        InputField("width", R.string.f_width_m, Uom.M, null),
                        InputField("thickness", R.string.f_thickness, Uom.MM, 150.0),
                        waste,
                    )
                }
            },
            compute = { i, o, bagKg ->
                val volume = when (o["shape"]) {
                    ConcreteShape.RECT_COLUMN -> i.v("width") / 1000 * i.v("depth") / 1000 * i.v("height") * i.v("count")
                    ConcreteShape.ROUND_COLUMN -> PI * (i.v("diameter") / 2000).let { it * it } * i.v("height") * i.v("count")
                    else -> i.v("length") * i.v("width") * i.v("thickness") / 1000
                } * (1 + i.v("waste") / 100)
                val total = o["mix"] ?: 7.0
                val (sandParts, aggParts) = concreteParts(total)
                val dry = volume * Factors.CONCRETE_DRY
                val cementKg = dry / total * Factors.CEMENT_DENSITY
                listOf(
                    ResultLine(R.string.r_volume, volume, Uom.M3, key = true),
                    ResultLine(R.string.r_cement, cementKg / bagKg, Uom.BAGS, whole = true, material = Material.CEMENT_BAG, key = true),
                    ResultLine(R.string.r_sand, dry * sandParts / total, Uom.M3, material = Material.SAND_M3),
                    ResultLine(R.string.r_aggregate, dry * aggParts / total, Uom.M3, material = Material.AGGREGATE_M3),
                    ResultLine(R.string.r_water, cementKg * Factors.WATER_CEMENT, Uom.LITRES),
                )
            },
        ),
        CalculatorSpec(
            type = CalcType.BLOCKS,
            options = listOf(OptionField("mortar", R.string.o_mortar, mortarChoices, defaultIndex = 2)),
            fields = {
                listOf(
                    InputField("length", R.string.f_wall_length, Uom.M, null),
                    InputField("height", R.string.f_wall_height, Uom.M, 3.0),
                    InputField("openings", R.string.f_openings, Uom.M2, 0.0, mustBePositive = false),
                    InputField("blockL", R.string.f_block_length, Uom.MM, 400.0),
                    InputField("blockH", R.string.f_block_height, Uom.MM, 200.0),
                    InputField("blockT", R.string.f_block_thickness, Uom.MM, 150.0),
                    InputField("joint", R.string.f_joint, Uom.MM, 10.0, mustBePositive = false),
                    waste,
                )
            },
            compute = { i, o, bagKg ->
                val area = i.v("length") * i.v("height") - i.v("openings")
                if (area <= 0) throw InvalidInputException("openings")
                val bl = i.v("blockL") / 1000
                val bh = i.v("blockH") / 1000
                val bt = i.v("blockT") / 1000
                val j = i.v("joint") / 1000
                val blocksExact = area / ((bl + j) * (bh + j))
                val mortarWet = (area * bt - blocksExact * bl * bh * bt).coerceAtLeast(0.0)
                listOf(
                    ResultLine(R.string.r_wall_area, area, Uom.M2),
                    ResultLine(R.string.r_blocks, blocksExact * (1 + i.v("waste") / 100), Uom.PCS, whole = true, material = Material.BLOCK, key = true),
                    ResultLine(R.string.r_mortar, mortarWet, Uom.M3),
                ) + cementSand(mortarWet * Factors.MORTAR_DRY * (1 + i.v("waste") / 100), o["mortar"] ?: 6.0, bagKg)
            },
        ),
        CalculatorSpec(
            type = CalcType.PLASTER,
            options = listOf(OptionField("mix", R.string.o_mortar, mortarChoices, defaultIndex = 0)),
            fields = {
                listOf(
                    InputField("area", R.string.f_area, Uom.M2, null),
                    InputField("thickness", R.string.f_thickness, Uom.MM, 12.0),
                    waste,
                )
            },
            compute = { i, o, bagKg ->
                val wet = i.v("area") * i.v("thickness") / 1000
                listOf(ResultLine(R.string.r_plaster_volume, wet, Uom.M3)) +
                    cementSand(wet * Factors.PLASTER_DRY * (1 + i.v("waste") / 100), o["mix"] ?: 4.0, bagKg)
            },
        ),
        CalculatorSpec(
            type = CalcType.TILES,
            options = emptyList(),
            fields = {
                listOf(
                    InputField("length", R.string.f_room_length, Uom.M, null),
                    InputField("width", R.string.f_room_width, Uom.M, null),
                    InputField("tileL", R.string.f_tile_length, Uom.MM, 600.0),
                    InputField("tileW", R.string.f_tile_width, Uom.MM, 600.0),
                    InputField("perBox", R.string.f_tiles_per_box, Uom.PCS, 4.0),
                    InputField("waste", R.string.f_waste, Uom.PERCENT, 10.0, mustBePositive = false),
                )
            },
            compute = { i, _, _ ->
                val area = i.v("length") * i.v("width")
                val tiles = ceil(area / (i.v("tileL") / 1000 * i.v("tileW") / 1000) * (1 + i.v("waste") / 100) - 1e-9)
                listOf(
                    ResultLine(R.string.r_floor_area, area, Uom.M2),
                    ResultLine(R.string.r_tiles, tiles, Uom.PCS, whole = true, key = true),
                    ResultLine(R.string.r_tile_boxes, tiles / i.v("perBox"), Uom.BOXES, whole = true, material = Material.TILE_BOX, key = true),
                )
            },
        ),
        CalculatorSpec(
            type = CalcType.PAINT,
            options = listOf(
                OptionField("coats", R.string.o_coats, listOf(Choice(R.string.coats_1, 1.0), Choice(R.string.coats_2, 2.0), Choice(R.string.coats_3, 3.0)), defaultIndex = 1),
            ),
            fields = {
                listOf(
                    InputField("area", R.string.f_wall_area, Uom.M2, null),
                    InputField("openings", R.string.f_openings, Uom.M2, 0.0, mustBePositive = false),
                    InputField("coverage", R.string.f_coverage, Uom.M2_PER_L, 10.0),
                    InputField("can", R.string.f_can_size, Uom.LITRES, 4.0),
                )
            },
            compute = { i, o, _ ->
                val area = i.v("area") - i.v("openings")
                if (area <= 0) throw InvalidInputException("openings")
                val litres = area * (o["coats"] ?: 2.0) / i.v("coverage")
                listOf(
                    ResultLine(R.string.r_paint_area, area, Uom.M2),
                    ResultLine(R.string.r_paint, litres, Uom.LITRES, key = true),
                    ResultLine(R.string.r_cans, litres / i.v("can"), Uom.CANS, whole = true, material = Material.PAINT_CAN, key = true),
                )
            },
        ),
        CalculatorSpec(
            type = CalcType.REBAR,
            options = listOf(
                OptionField(
                    "diameter", R.string.o_bar_diameter,
                    listOf(8.0, 10.0, 12.0, 16.0, 20.0, 25.0).map { Choice(R.string.bar_y, it) },
                    defaultIndex = 2,
                ),
            ),
            fields = {
                listOf(
                    InputField("length", R.string.f_bar_length, Uom.M, null),
                    InputField("count", R.string.f_bar_count, Uom.COUNT, null),
                    InputField("stock", R.string.f_stock_length, Uom.M, 12.0),
                )
            },
            compute = { i, o, _ ->
                val d = o["diameter"] ?: 12.0
                val totalLength = i.v("length") * i.v("count")
                val kg = d * d / 162.0 * totalLength
                listOf(
                    ResultLine(R.string.r_total_length, totalLength, Uom.M),
                    ResultLine(R.string.r_steel_weight, kg, Uom.KG, material = Material.STEEL_KG, key = true),
                    ResultLine(R.string.r_stock_bars, totalLength / i.v("stock"), Uom.BARS, whole = true, key = true),
                )
            },
        ),
        CalculatorSpec(
            type = CalcType.EXCAVATION,
            options = emptyList(),
            fields = {
                listOf(
                    InputField("length", R.string.f_length, Uom.M, null),
                    InputField("width", R.string.f_width_m, Uom.M, null),
                    InputField("depth", R.string.f_depth_m, Uom.M, null),
                    InputField("swell", R.string.f_swell, Uom.PERCENT, 25.0, mustBePositive = false),
                    InputField("truck", R.string.f_truck, Uom.M3, 10.0),
                )
            },
            compute = { i, _, _ ->
                val bank = i.v("length") * i.v("width") * i.v("depth")
                val loose = bank * (1 + i.v("swell") / 100)
                listOf(
                    ResultLine(R.string.r_bank_volume, bank, Uom.M3, key = true),
                    ResultLine(R.string.r_loose_volume, loose, Uom.M3),
                    ResultLine(R.string.r_trucks, loose / i.v("truck"), Uom.TRUCKS, whole = true, material = Material.TRUCK, key = true),
                )
            },
        ),
        CalculatorSpec(
            type = CalcType.ROOFING,
            options = listOf(
                OptionField("sides", R.string.o_roof_sides, listOf(Choice(R.string.roof_mono, 1.0), Choice(R.string.roof_gable, 2.0)), defaultIndex = 1),
            ),
            fields = {
                listOf(
                    InputField("length", R.string.f_roof_length, Uom.M, null),
                    InputField("rafter", R.string.f_rafter, Uom.M, null),
                    InputField("cover", R.string.f_sheet_cover, Uom.MM, 800.0),
                    InputField("sheetLen", R.string.f_sheet_length, Uom.M, 3.0),
                    InputField("lap", R.string.f_end_lap, Uom.MM, 150.0, mustBePositive = false),
                )
            },
            compute = { i, o, _ ->
                val lap = i.v("lap") / 1000
                val sheetLen = i.v("sheetLen")
                if (lap >= sheetLen) throw InvalidInputException("lap")
                val across = ceil(i.v("length") / (i.v("cover") / 1000) - 1e-9)
                val rows = if (i.v("rafter") <= sheetLen) 1.0 else ceil((i.v("rafter") - lap) / (sheetLen - lap) - 1e-9)
                val sides = o["sides"] ?: 2.0
                listOf(
                    ResultLine(R.string.r_roof_area, i.v("length") * i.v("rafter") * sides, Uom.M2),
                    ResultLine(R.string.r_sheets_across, across, Uom.SHEETS, whole = true),
                    ResultLine(R.string.r_sheet_rows, rows, Uom.COUNT, whole = true),
                    ResultLine(R.string.r_sheets, across * rows * sides, Uom.SHEETS, whole = true, material = Material.SHEET, key = true),
                )
            },
        ),
    ).associateBy { it.type }

    fun spec(type: CalcType): CalculatorSpec = all.getValue(type)

    fun defaultOptions(type: CalcType): Map<String, Double> =
        spec(type).options.associate { it.key to it.choices[it.defaultIndex].value }

    /**
     * Parses raw text inputs and computes results.
     * Throws [InvalidInputException] naming the first bad field.
     */
    fun calculate(
        type: CalcType,
        rawInputs: Map<String, String>,
        options: Map<String, Double>,
        bagKg: Double,
        prices: Map<Material, Double>,
    ): CalcResult {
        val spec = spec(type)
        val opts = defaultOptions(type) + options
        val values = spec.fields(opts).associate { f ->
            val v = parseNumber(rawInputs[f.key]) ?: throw InvalidInputException(f.key)
            if (v < 0 || (f.mustBePositive && v <= 0) || v > 1_000_000) throw InvalidInputException(f.key)
            if (f.unit == Uom.PERCENT && v > 100) throw InvalidInputException(f.key)
            f.key to v
        }
        val lines = spec.compute(values, opts, bagKg)
        val cost = lines.sumOf { line -> line.material?.let { line.shownValue * (prices[it] ?: 0.0) } ?: 0.0 }
        return CalcResult(lines, cost)
    }

    /** Accepts "1.5", "1,5" and surrounding spaces. */
    fun parseNumber(text: String?): Double? {
        val t = text?.trim()?.replace(',', '.') ?: return null
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun Map<String, Double>.v(key: String): Double = getValue(key)
}

/** Materials a calculator can price, in display order. */
fun CalcType.materials(): List<Material> = when (this) {
    CalcType.CONCRETE -> listOf(Material.CEMENT_BAG, Material.SAND_M3, Material.AGGREGATE_M3)
    CalcType.BLOCKS -> listOf(Material.BLOCK, Material.CEMENT_BAG, Material.SAND_M3)
    CalcType.PLASTER -> listOf(Material.CEMENT_BAG, Material.SAND_M3)
    CalcType.TILES -> listOf(Material.TILE_BOX)
    CalcType.PAINT -> listOf(Material.PAINT_CAN)
    CalcType.REBAR -> listOf(Material.STEEL_KG)
    CalcType.EXCAVATION -> listOf(Material.TRUCK)
    CalcType.ROOFING -> listOf(Material.SHEET)
}
