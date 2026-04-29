package com.example.blindspotnews.ui

import androidx.compose.ui.graphics.Color

object AppColors {

    // LIGHT MODE
    val SoftYellow = Color(0xFFFFE680)
    val LightText = Color(0xFF111111)
    val LightCard = Color.White

    // DARK MODE
    val DarkBackground = Color(0xFF121212)
    val DarkText = Color(0xFFF5F5F5)
    val DarkCard = Color(0xFF1E1E1E)

    // ACCENT
    val AccentYellow = Color(0xFFFFD54F)

    fun background() =
        if (AppThemeState.isDarkMode) DarkBackground else SoftYellow

    fun text() =
        if (AppThemeState.isDarkMode) DarkText else LightText

    fun card() =
        if (AppThemeState.isDarkMode) DarkCard else LightCard

    fun buttonBackground() =
        if (AppThemeState.isDarkMode) AccentYellow else LightText

    fun buttonText() =
        if (AppThemeState.isDarkMode) LightText else AccentYellow
}