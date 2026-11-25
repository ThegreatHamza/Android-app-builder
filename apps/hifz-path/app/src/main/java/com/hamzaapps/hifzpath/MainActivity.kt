package com.hamzaapps.hifzpath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzaapps.hifzpath.ui.HifzRoot
import com.hamzaapps.hifzpath.ui.HifzViewModel
import com.hamzaapps.hifzpath.ui.theme.HifzTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HifzViewModel by viewModels { HifzViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by viewModel.themeMode.collectAsStateWithLifecycle()
            HifzTheme(themeMode = theme) {
                HifzRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDay()
    }
}
