package com.aistudio.trueidle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.trueidle.ui.TrueIdleApp
import com.aistudio.trueidle.ui.theme.TrueIdleTheme
import com.aistudio.trueidle.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                viewModel.onForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                viewModel.onBackground()
            }
        })
        setContent {
            val state by viewModel.gameState.collectAsStateWithLifecycle()
            TrueIdleTheme(isDarkMode = state.isDarkMode) {
                TrueIdleApp(viewModel = viewModel)
            }
        }
    }
}
