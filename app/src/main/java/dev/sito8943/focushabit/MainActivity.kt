package dev.sito8943.focushabit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.sito8943.focushabit.ui.MainScreen
import dev.sito8943.focushabit.ui.theme.FocusHabitTheme

/** Android composition root only. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FocusHabitApp).container
        setContent {
            FocusHabitTheme {
                MainScreen(container)
            }
        }
    }
}
