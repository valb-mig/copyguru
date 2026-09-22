package com.valb.copyguru.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF6C5CE7)

private val LightColors = lightColorScheme(
    primary = Brand,
    secondary = Color(0xFF00BB94),
    background = Color(0xFFF7F7FB),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9C8CFF),
    secondary = Color(0xFF55EFC4),
    background = Color(0xFF121218),
    surface = Color(0xFF1B1B22)
)

@Composable
fun CopyGuruTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
