package com.hamzaapps.hifzpath.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hamzaapps.hifzpath.AppContainer
import com.hamzaapps.hifzpath.HifzApp
import com.hamzaapps.hifzpath.R
import com.hamzaapps.hifzpath.backup.InvalidBackupException
import com.hamzaapps.hifzpath.billing.Monetization
import com.hamzaapps.hifzpath.data.db.Student
import com.hamzaapps.hifzpath.domain.DailyPlan
import com.hamzaapps.hifzpath.domain.Direction
import com.hamzaapps.hifzpath.domain.LogEntry
import com.hamzaapps.hifzpath.domain.PageState
import com.hamzaapps.hifzpath.domain.PlanGenerator
import com.hamzaapps.hifzpath.domain.PlanSettings
import com.hamzaapps.hifzpath.domain.Stats
import com.hamzaapps.hifzpath.domain.StatsCalculator
import com.hamzaapps.hifzpath.domain.Strength
import com.hamzaapps.hifzpath.util.DayClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HifzUiState(
    val loading: Boolean = true,
    val students: List<Student> = emptyList(),
    val student: Student? = null,
    val pages: Map<Int, PageState> = emptyMap(),
    val logs: List<LogEntry> = emptyList(),
    val plan: DailyPlan? = null,
    val stats: Stats? = null,
    val today: Long = 0,
)

data class UiMessage(@param:StringRes val text: Int, val arg: Any? = null)

fun Student.planSettings() = PlanSettings(
    direction = if (direction == 1) Direction.FROM_END else Direction.FROM_START,
    newQuartersPerDay = newQuartersPerDay,
    recentDays = recentDays,
    cycleDays = cycleDays,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HifzViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.repository
    private val today = MutableStateFlow(DayClock.today())

    val themeMode: StateFlow<Int> = c.settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val isPro: StateFlow<Boolean> = c.billing.isPro
    val proPrice: StateFlow<String?> = c.billing.price

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages

    val state: StateFlow<HifzUiState> =
        combine(repo.allStudents, c.settings.activeStudentId) { list, id ->
            list to (list.firstOrNull { it.id == id } ?: list.firstOrNull())
        }.flatMapLatest { (list, student) ->
            if (student == null) {
                flowOf(HifzUiState(loading = false, students = list, today = today.value))
            } else {
                combine(repo.pageStates(student.id), repo.logEntries(student.id), today) { pages, logs, day ->
                    HifzUiState(
                        loading = false,
                        students = list,
                        student = student,
                        pages = pages,
                        logs = logs,
                        plan = PlanGenerator.generate(pages, logs.filter { it.day == day }, student.planSettings(), day),
                        stats = StatsCalculator.compute(pages, logs, day),
                        today = day,
                    )
                }
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HifzUiState(today = today.value))

    /** Call when the app returns to the foreground, so "today" rolls over after midnight. */
    fun refreshDay() {
        today.value = DayClock.today()
        c.billing.connect()
    }

    private val studentId: Long? get() = state.value.student?.id

    private fun withStudent(block: suspend (Long) -> Unit) {
        val id = studentId ?: return
        viewModelScope.launch { block(id) }
    }

    // ---- Students ----
    fun canAddStudent(): Boolean = isPro.value || state.value.students.size < Monetization.FREE_STUDENT_LIMIT

    fun createStudent(name: String, direction: Int, quarters: Int, cycleDays: Int) {
        viewModelScope.launch {
            repo.createStudent(
                Student(
                    name = name,
                    direction = direction,
                    newQuartersPerDay = quarters,
                    cycleDays = cycleDays,
                    createdEpochDay = DayClock.today(),
                ),
            )
        }
    }

    fun updatePlan(direction: Int, quarters: Int, cycleDays: Int) {
        val s = state.value.student ?: return
        viewModelScope.launch {
            repo.updateStudent(s.copy(direction = direction, newQuartersPerDay = quarters, cycleDays = cycleDays))
            _messages.emit(UiMessage(R.string.msg_plan_saved))
        }
    }

    fun renameStudent(student: Student, name: String) {
        viewModelScope.launch { repo.updateStudent(student.copy(name = name)) }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repo.deleteStudent(student.id)
            val next = state.value.students.firstOrNull { it.id != student.id }
            if (next != null) c.settings.setActiveStudent(next.id)
        }
    }

    fun switchStudent(student: Student) {
        viewModelScope.launch { c.settings.setActiveStudent(student.id) }
    }

    // ---- Pages ----
    fun markMemorized(page: Int, strength: Strength) = withStudent {
        repo.markMemorized(it, page, strength, today.value)
        _messages.emit(UiMessage(R.string.msg_page_memorized, page))
    }

    fun undoMemorized(page: Int) = withStudent { repo.undoMemorized(it, page, today.value) }
    fun markLearning(page: Int) = withStudent { repo.markLearning(it, page) }
    fun setStrength(page: Int, strength: Strength) = withStudent { repo.setStrength(it, page, strength) }
    fun review(page: Int, quality: Strength) = withStudent { repo.recordReview(it, page, quality, today.value) }
    fun undoReview(page: Int) = withStudent { repo.undoReview(it, page, today.value) }
    fun clearPage(page: Int) = withStudent { repo.clearPages(it, listOf(page)) }

    fun markKnown(pages: Iterable<Int>) = withStudent {
        repo.markKnown(it, pages)
        _messages.emit(UiMessage(R.string.msg_marked_known))
    }

    fun markLearningAll(pages: Iterable<Int>) = withStudent { repo.markLearningAll(it, pages) }
    fun clearPages(pages: Iterable<Int>) = withStudent { repo.clearPages(it, pages) }

    // ---- Settings ----
    fun setTheme(mode: Int) {
        viewModelScope.launch { c.settings.setThemeMode(mode) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = c.backup.exportJson()
                withContext(Dispatchers.IO) {
                    c.appContext.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                        ?: error("Cannot open file")
                }
            }.isSuccess
            _messages.emit(UiMessage(if (ok) R.string.msg_export_ok else R.string.msg_export_failed))
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    c.appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: error("Cannot open file")
                }
                val count = c.backup.importJson(text)
                c.backup.parse(text).students.firstOrNull()?.let { c.settings.setActiveStudent(it.id) }
                count
            }
            val message = result.fold(
                onSuccess = { UiMessage(R.string.msg_import_ok, it) },
                onFailure = { UiMessage(if (it is InvalidBackupException) R.string.msg_import_invalid else R.string.msg_import_failed) },
            )
            _messages.emit(message)
        }
    }

    fun restorePurchases() {
        c.billing.refreshPurchases()
        viewModelScope.launch { _messages.emit(UiMessage(R.string.msg_restore_checked)) }
    }

    fun buyPro(activity: android.app.Activity) {
        if (!c.billing.launchPurchase(activity)) {
            viewModelScope.launch { _messages.emit(UiMessage(R.string.msg_billing_unavailable)) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HifzViewModel((this[APPLICATION_KEY] as HifzApp).container) }
        }
    }
}
