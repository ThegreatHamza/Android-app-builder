package com.hamzaapps.sitecalc.ui

import android.app.Activity
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hamzaapps.sitecalc.AppContainer
import com.hamzaapps.sitecalc.R
import com.hamzaapps.sitecalc.SiteCalcApp
import com.hamzaapps.sitecalc.backup.InvalidBackupException
import com.hamzaapps.sitecalc.backup.Report
import com.hamzaapps.sitecalc.backup.ReportWriter
import com.hamzaapps.sitecalc.billing.Monetization
import com.hamzaapps.sitecalc.data.db.Project
import com.hamzaapps.sitecalc.data.db.ProjectSummary
import com.hamzaapps.sitecalc.data.db.SavedCalc
import com.hamzaapps.sitecalc.domain.CalcType
import com.hamzaapps.sitecalc.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

data class UiMessage(@param:StringRes val text: Int, val arg: Any? = null)

class MainViewModel(val c: AppContainer) : ViewModel() {
    private val res get() = c.appContext.resources

    val themeMode: StateFlow<Int> = c.settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val currency: StateFlow<String> = c.settings.currency.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val bagKg: StateFlow<Double> = c.settings.bagKg.stateIn(viewModelScope, SharingStarted.Eagerly, 50.0)
    val launchCount: StateFlow<Int> = c.settings.launchCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val isPro: StateFlow<Boolean> = c.billing.isPro
    val proPrice: StateFlow<String?> = c.billing.price
    val canRequestAds: StateFlow<Boolean> = c.ads.canRequestAds
    val privacyOptionsRequired: StateFlow<Boolean> = c.ads.privacyOptionsRequired
    val projects: StateFlow<List<ProjectSummary>?> =
        c.projects.summaries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages

    /** Text to hand to the Android share sheet. */
    private val _share = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val share: SharedFlow<String> = _share

    fun project(id: Long): Flow<Project?> = c.projects.project(id)
    fun items(projectId: Long): Flow<List<SavedCalc>> = c.projects.items(projectId)

    fun onLaunch(activity: Activity, firstCreate: Boolean) {
        viewModelScope.launch {
            if (firstCreate) c.settings.incrementLaunches()
            c.billing.connect()
            c.ads.gatherConsent(activity, c.settings.proUnlocked.first())
        }
    }

    fun onResume() = c.billing.connect()

    fun canCreateProject(): Boolean = isPro.value || (projects.value?.size ?: 0) < Monetization.FREE_PROJECT_LIMIT

    fun createProject(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(c.projects.createProject(name)) }
    }

    fun renameProject(project: Project, name: String) {
        viewModelScope.launch { c.projects.renameProject(project, name) }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            c.projects.deleteProject(id)
            _messages.emit(UiMessage(R.string.msg_project_deleted))
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { c.projects.deleteItem(id) }
    }

    // ---- Settings ----
    fun setTheme(mode: Int) = viewModelScope.launch { c.settings.setThemeMode(mode) }
    fun setCurrency(value: String) = viewModelScope.launch { c.settings.setCurrency(value) }
    fun setBagKg(value: Double) = viewModelScope.launch { c.settings.setBagKg(value) }

    fun buyPro(activity: Activity) {
        if (!c.billing.launchPurchase(activity)) {
            viewModelScope.launch { _messages.emit(UiMessage(R.string.msg_billing_unavailable)) }
        }
    }

    fun restorePurchases() {
        c.billing.refreshPurchases()
        viewModelScope.launch { _messages.emit(UiMessage(R.string.msg_restore_checked)) }
    }

    fun showPrivacyOptions(activity: Activity) = c.ads.showPrivacyOptions(activity)

    // ---- Backup & reports ----
    private suspend fun writeTo(uri: Uri, block: (java.io.OutputStream) -> Unit) = withContext(Dispatchers.IO) {
        c.appContext.contentResolver.openOutputStream(uri, "wt")?.use(block) ?: error("Cannot open file")
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            val json = c.backup.exportJson()
            writeTo(uri) { it.write(json.toByteArray()) }
        }.isSuccess
        _messages.emit(UiMessage(if (ok) R.string.msg_export_ok else R.string.msg_export_failed))
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            val text = withContext(Dispatchers.IO) {
                c.appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Cannot open file")
            }
            c.backup.importJson(text)
        }
        _messages.emit(
            result.fold(
                onSuccess = { UiMessage(R.string.msg_import_ok, it) },
                onFailure = { UiMessage(if (it is InvalidBackupException) R.string.msg_import_invalid else R.string.msg_import_failed) },
            ),
        )
    }

    private suspend fun buildReport(projectId: Long): Report? {
        val project = c.projects.allProjects().firstOrNull { it.id == projectId } ?: return null
        val items = c.projects.itemsOf(projectId)
        val cur = currency.value
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date())
        return Report(
            title = project.name,
            subtitle = res.getString(R.string.report_subtitle, res.getString(R.string.app_name), date),
            headers = listOf(
                res.getString(R.string.report_col_item),
                res.getString(R.string.report_col_calculator),
                res.getString(R.string.report_col_details),
                res.getString(R.string.report_col_cost),
            ),
            rows = items.map { item ->
                val type = runCatching { CalcType.valueOf(item.type) }.getOrNull()
                listOf(
                    item.title,
                    type?.let { res.getString(it.title) } ?: item.type,
                    item.summary,
                    if (item.cost > 0) Fmt.money(item.cost, cur) else "",
                )
            },
            totalLabel = res.getString(R.string.total_cost),
            total = Fmt.money(items.sumOf { it.cost }, cur),
            footer = res.getString(R.string.report_disclaimer),
        )
    }

    fun shareProject(projectId: Long) = viewModelScope.launch {
        val r = buildReport(projectId) ?: return@launch
        val text = buildString {
            appendLine(r.title)
            appendLine(r.subtitle)
            appendLine()
            r.rows.forEachIndexed { i, row ->
                appendLine("${i + 1}. ${row[0]} (${row[1]})")
                appendLine(row[2])
                if (row[3].isNotBlank()) appendLine(row[3])
                appendLine()
            }
            appendLine("${r.totalLabel}: ${r.total}")
            appendLine()
            append(r.footer)
        }
        _share.emit(text)
    }

    fun exportCsv(projectId: Long, uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            val r = buildReport(projectId) ?: error("missing")
            writeTo(uri) { it.write(ReportWriter.csv(r).toByteArray()) }
        }.isSuccess
        _messages.emit(UiMessage(if (ok) R.string.msg_export_ok else R.string.msg_export_failed))
    }

    fun exportPdf(projectId: Long, uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            val r = buildReport(projectId) ?: error("missing")
            writeTo(uri) { ReportWriter.pdf(r, it) }
        }.isSuccess
        _messages.emit(UiMessage(if (ok) R.string.msg_export_ok else R.string.msg_export_failed))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MainViewModel((this[APPLICATION_KEY] as SiteCalcApp).container) }
        }
    }
}
