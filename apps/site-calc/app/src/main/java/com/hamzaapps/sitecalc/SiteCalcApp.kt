package com.hamzaapps.sitecalc

import android.app.Application
import android.content.Context
import com.hamzaapps.sitecalc.ads.AdsManager
import com.hamzaapps.sitecalc.backup.BackupManager
import com.hamzaapps.sitecalc.billing.BillingManager
import com.hamzaapps.sitecalc.data.ProjectRepository
import com.hamzaapps.sitecalc.data.SettingsRepository
import com.hamzaapps.sitecalc.data.db.SiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SiteCalcApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database = SiteDatabase.build(context)
    val settings = SettingsRepository(context)
    val projects = ProjectRepository(database)
    val billing = BillingManager(context, settings, appScope)
    val ads = AdsManager(context)
    val backup = BackupManager(database)
}
