package com.hamzaapps.sitecalc.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamzaapps.sitecalc.AppContainer
import com.hamzaapps.sitecalc.R
import com.hamzaapps.sitecalc.data.ProjectRepository
import com.hamzaapps.sitecalc.data.db.SavedCalc
import com.hamzaapps.sitecalc.domain.CalcResult
import com.hamzaapps.sitecalc.domain.CalcType
import com.hamzaapps.sitecalc.domain.Calculators
import com.hamzaapps.sitecalc.domain.InvalidInputException
import com.hamzaapps.sitecalc.domain.Material
import com.hamzaapps.sitecalc.util.Fmt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CalcOutcome {
    data object Empty : CalcOutcome
    data class Invalid(val fieldKey: String) : CalcOutcome
    data class Ok(val result: CalcResult) : CalcOutcome
}

class CalculatorViewModel(
    private val c: AppContainer,
    val type: CalcType,
    private val itemId: Long?,
) : ViewModel() {
    private val spec = Calculators.spec(type)

    private val _inputs = MutableStateFlow(
        spec.fields(Calculators.defaultOptions(type)).associate { f -> f.key to (f.default?.let { Fmt.number(it) } ?: "") },
    )
    val inputs: StateFlow<Map<String, String>> = _inputs

    private val _options = MutableStateFlow(Calculators.defaultOptions(type))
    val options: StateFlow<Map<String, Double>> = _options

    private val _priceText = MutableStateFlow<Map<Material, String>>(emptyMap())
    val priceText: StateFlow<Map<Material, String>> = _priceText

    /** Item being edited, if any. */
    private val _editing = MutableStateFlow<SavedCalc?>(null)
    val editing: StateFlow<SavedCalc?> = _editing

    private val _saved = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    /** Emits the project id after a successful save. */
    val saved: SharedFlow<Long> = _saved

    val outcome: StateFlow<CalcOutcome> = combine(_inputs, _options, c.settings.bagKg, c.settings.prices) { i, o, bag, prices ->
        val anyEntered = spec.fields(o).any { f -> f.default == null && i[f.key]?.isNotBlank() == true } ||
            spec.fields(o).all { it.default != null }
        if (!anyEntered) {
            CalcOutcome.Empty
        } else {
            try {
                CalcOutcome.Ok(Calculators.calculate(type, i, o, bag, prices))
            } catch (e: InvalidInputException) {
                CalcOutcome.Invalid(e.fieldKey)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CalcOutcome.Empty)

    init {
        viewModelScope.launch {
            val prices = c.settings.prices.first()
            _priceText.value = prices.mapValues { Fmt.number(it.value) }
            if (itemId != null) {
                c.projects.item(itemId)?.let { item ->
                    _editing.value = item
                    _options.value = Calculators.defaultOptions(type) + ProjectRepository.decodeOptions(item.optionsJson)
                    _inputs.value = _inputs.value + ProjectRepository.decodeInputs(item.inputsJson)
                }
            }
        }
    }

    fun setInput(key: String, value: String) {
        _inputs.value = _inputs.value + (key to value.take(12))
    }

    fun setOption(key: String, value: Double) {
        val newOptions = _options.value + (key to value)
        _options.value = newOptions
        // Fill defaults for fields that appear with the new option.
        val current = _inputs.value
        _inputs.value = current + spec.fields(newOptions)
            .filter { it.key !in current }
            .associate { f -> f.key to (f.default?.let { Fmt.number(it) } ?: "") }
    }

    fun setPrice(material: Material, text: String) {
        val t = text.take(12)
        _priceText.value = _priceText.value + (material to t)
        val parsed = Calculators.parseNumber(t)
        if (t.isBlank() || (parsed != null && parsed >= 0)) {
            viewModelScope.launch { c.settings.setPrice(material, parsed) }
        }
    }

    /** Key result lines as plain text, e.g. "Cement: 12 bags". */
    private fun summaryOf(result: CalcResult): String {
        val res = c.appContext.resources
        return result.lines.joinToString("\n") { line ->
            res.getString(
                R.string.summary_line,
                res.getString(line.label),
                Fmt.number(line.shownValue, line.whole),
                res.getString(line.unit.label),
            ).trim()
        }
    }

    /** Saves into an existing project, or a new one when [newProjectName] is given. */
    fun save(projectId: Long?, newProjectName: String?, title: String) {
        val result = (outcome.value as? CalcOutcome.Ok)?.result ?: return
        val options = _options.value
        val fieldKeys = spec.fields(options).map { it.key }.toSet()
        viewModelScope.launch {
            val pid = projectId ?: c.projects.createProject(newProjectName ?: return@launch)
            val existing = _editing.value
            val item = SavedCalc(
                id = existing?.takeIf { it.projectId == pid }?.id ?: 0,
                projectId = pid,
                type = type.name,
                title = title.trim().ifBlank { c.appContext.getString(type.title) },
                inputsJson = ProjectRepository.encodeInputs(_inputs.value.filterKeys { it in fieldKeys }),
                optionsJson = ProjectRepository.encodeOptions(options),
                summary = summaryOf(result),
                cost = result.cost,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val id = c.projects.saveItem(item)
            _editing.value = item.copy(id = id)
            _saved.emit(pid)
        }
    }
}
