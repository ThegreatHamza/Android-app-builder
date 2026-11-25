package com.hamzaapps.sitecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzaapps.sitecalc.ui.MainViewModel
import com.hamzaapps.sitecalc.ui.SiteCalcRoot
import com.hamzaapps.sitecalc.ui.theme.SiteCalcTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.onLaunch(this, firstCreate = savedInstanceState == null)
        setContent {
            val theme by viewModel.themeMode.collectAsStateWithLifecycle()
            SiteCalcTheme(theme) { SiteCalcRoot(viewModel) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
