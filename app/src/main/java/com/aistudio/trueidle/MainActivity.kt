package com.aistudio.trueidle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aistudio.trueidle.ui.TrueIdleApp
import com.aistudio.trueidle.ui.theme.TrueIdleTheme
import com.aistudio.trueidle.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrueIdleTheme {
                TrueIdleApp(viewModel = viewModel)
            }
        }
    }
}
