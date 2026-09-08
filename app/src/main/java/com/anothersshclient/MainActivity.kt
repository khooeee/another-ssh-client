package com.anothersshclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anothersshclient.data.TerminalPreferences
import com.anothersshclient.ui.AnotherSshClientApp
import com.anothersshclient.ui.theme.AnotherSshClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { TerminalPreferences(this) }
            val themeMode by prefs.themeModeFlow().collectAsStateWithLifecycle(prefs.themeMode)

            AnotherSshClientTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnotherSshClientApp(
                        themeMode = themeMode,
                        onCycleThemeMode = { prefs.themeMode = themeMode.next() },
                    )
                }
            }
        }
    }
}
