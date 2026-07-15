package com.example.mydailyactivity.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FocusTealLight,
    onPrimary = FocusTealDark,
    secondary = RewardGoldLight,
    tertiary = RewardGoldLight,
    background = Color(0xFF101918),
    surface = Color(0xFF182321),
    surfaceVariant = Color(0xFF273331),
    onBackground = Color(0xFFEAF3F1),
    onSurface = Color(0xFFEAF3F1),
    onSurfaceVariant = Color(0xFFC5D3D0),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = FocusTeal,
    onPrimary = Color.White,
    primaryContainer = FocusTealLight,
    onPrimaryContainer = FocusTealDark,
    secondary = RewardGold,
    onSecondary = Color.White,
    secondaryContainer = RewardGoldLight,
    onSecondaryContainer = Color(0xFF3F2500),
    tertiary = RewardGold,
    background = CalmBackground,
    surface = CalmSurface,
    surfaceVariant = CalmSurfaceVariant,
    onBackground = CalmText,
    onSurface = CalmText,
    onSurfaceVariant = CalmTextMuted,
    outline = Color(0xFF8AA09C),
    error = CalmError
)

@Composable
fun MyDailyActivityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
