package com.hamzaapps.hifzpath

import android.app.Application
import android.content.Context
import com.hamzaapps.hifzpath.backup.BackupManager
import com.hamzaapps.hifzpath.billing.BillingManager
import com.hamzaapps.hifzpath.data.HifzRepository
import com.hamzaapps.hifzpath.data.SettingsRepository
import com.hamzaapps.hifzpath.data.db.HifzDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

class HifzApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Simple manual dependency container. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database: HifzDatabase = HifzDatabase.build(context)
    val settings = SettingsRepository(context)
    val repository = HifzRepository(database, settings)
    val billing = BillingManager(context, settings, appScope)
    val backup = BackupManager(database)
}
