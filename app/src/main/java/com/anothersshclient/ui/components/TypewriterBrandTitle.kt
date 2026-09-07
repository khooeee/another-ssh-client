package com.anothersshclient.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.anothersshclient.ui.BrandIntro
import kotlinx.coroutines.delay

private const val BrandTitle = "Another SSH Client"
private const val TypeDelayMs = 55L
private const val CursorBlinkMs = 530L
private const val CursorHoldAfterTypeMs = 3_000L

@Composable
fun TypewriterBrandTitle() {
    val style = MaterialTheme.typography.headlineMedium
    val ink = MaterialTheme.colorScheme.onSurface

    var visibleChars by remember {
        mutableIntStateOf(if (BrandIntro.hasCompleted) BrandTitle.length else 0)
    }
    var showCursor by remember { mutableStateOf(!BrandIntro.hasCompleted) }
    var cursorLit by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (BrandIntro.hasCompleted) {
            visibleChars = BrandTitle.length
            showCursor = false
            return@LaunchedEffect
        }
        try {
            for (i in 1..BrandTitle.length) {
                visibleChars = i
                delay(TypeDelayMs)
            }
            delay(CursorHoldAfterTypeMs)
        } finally {
            showCursor = false
            visibleChars = BrandTitle.length
            BrandIntro.hasCompleted = true
        }
    }

    LaunchedEffect(showCursor) {
        if (!showCursor) return@LaunchedEffect
        cursorLit = true
        while (true) {
            delay(CursorBlinkMs)
            cursorLit = !cursorLit
        }
    }

    Text(
        text = buildAnnotatedString {
            append(BrandTitle.take(visibleChars))
            if (showCursor) {
                // Thick underscore-style caret (lower eighth block reads heavier than "_").
                withStyle(SpanStyle(color = if (cursorLit) ink else Color.Transparent)) {
                    append("▁")
                }
            }
        },
        style = style,
        color = ink,
        maxLines = 1,
    )
}
