package io.github.ieswar23.buddyup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.ieswar23.buddyup.ui.BuddyUpApp
import io.github.ieswar23.buddyup.ui.LaunchState
import io.github.ieswar23.buddyup.ui.MainViewModel
import io.github.ieswar23.buddyup.ui.theme.BuddyUpTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.launchState.value is LaunchState.Loading }
        enableEdgeToEdge()

        setContent {
            val launchState by viewModel.launchState.collectAsStateWithLifecycle()
            val chrome by viewModel.chrome.collectAsStateWithLifecycle()
            BuddyUpTheme(themeMode = chrome.themeMode) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val state = launchState
                    if (state is LaunchState.Ready) {
                        BuddyUpApp(onboardingComplete = state.onboardingComplete, viewModel = viewModel)
                    }
                }
            }
        }
    }
}
