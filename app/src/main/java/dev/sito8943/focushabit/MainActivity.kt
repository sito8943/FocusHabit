package dev.sito8943.focushabit

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sito8943.focushabit.core.settings.AppSettings
import dev.sito8943.focushabit.core.settings.ThemeMode
import dev.sito8943.focushabit.ui.MainScreen
import dev.sito8943.focushabit.ui.theme.FocusHabitTheme

/** Android composition root only. */
class MainActivity : ComponentActivity() {

    /** Language chosen in Settings (recreated when it changes). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FocusHabitApp).container
        setContent {
            val theme by container.settings.theme.collectAsStateWithLifecycle()
            val dark = when (theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Status/navigation bar icons follow the app theme, not only the system one.
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
            }
            FocusHabitTheme(darkTheme = dark) {
                MainScreen(container)
            }
        }
    }
}
