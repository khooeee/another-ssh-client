package com.anothersshclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.anothersshclient.ui.SsherApp
import com.anothersshclient.ui.theme.SsherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SsherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SsherApp()
                }
            }
        }
    }
}
