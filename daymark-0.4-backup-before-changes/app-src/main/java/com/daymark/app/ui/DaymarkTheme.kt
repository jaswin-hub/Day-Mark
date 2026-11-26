package com.daymark.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import android.animation.ValueAnimator

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class DaymarkPalette(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSecondary: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val upcoming: Color,
    val missed: Color,
    val completed: Color,
    val dueToday: Color,
    val neutral: Color,
    val selected: Color
)

object DaymarkPalettes {
    val Light = DaymarkPalette(
        background = Color(0xFFF2EEE3), surface = Color(0xFFFFFCF4), surfaceRaised = Color(0xFFFFFCF4),
        surfaceSecondary = Color(0xFFE8E1D3), border = Color(0xFFD8D0C1), textPrimary = Color(0xFF302F27),
        textSecondary = Color(0xFF625F53), textMuted = Color(0xFF625F53), accent = Color(0xFF586D2C),
        upcoming = Color(0xFF405A83), missed = Color(0xFFA23C41), completed = Color(0xFF2F704B),
        dueToday = Color(0xFF805516), neutral = Color(0xFF625F53), selected = Color(0xFFDCE2C8)
    )
    val Dark = DaymarkPalette(
        background = Color(0xFF20251F), surface = Color(0xFF2A3028), surfaceRaised = Color(0xFF343B30),
        surfaceSecondary = Color(0xFF343B30), border = Color(0xFF41483F), textPrimary = Color(0xFFF3F1E8),
        textSecondary = Color(0xFFBCC1AE), textMuted = Color(0xFFBCC1AE), accent = Color(0xFFC3D64A),
        upcoming = Color(0xFF9DB5D4), missed = Color(0xFFD98D90), completed = Color(0xFF86B89C),
        dueToday = Color(0xFFD8B776), neutral = Color(0xFFBCC1AE), selected = Color(0xFF41483F)
    )
}

object DaymarkMotion {
    const val MICRO = 150
    const val CONTENT = 210
    const val SCREEN = 260
    const val EXPAND = 250
}

fun motionDuration(requested: Int): Int = if (ValueAnimator.areAnimatorsEnabled()) requested else 0

fun <T> daymarkTween(durationMillis: Int) = tween<T>(durationMillis = motionDuration(durationMillis))

val LocalDaymarkPalette = staticCompositionLocalOf { DaymarkPalettes.Light }

@Composable
fun DaymarkTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DaymarkPalettes.Dark else DaymarkPalettes.Light
    val scheme = if (dark) darkColorScheme(
        primary = palette.accent, onPrimary = Color(0xFF1B222B), secondary = palette.completed,
        background = palette.background, onBackground = palette.textPrimary, surface = palette.surface,
        onSurface = palette.textPrimary, surfaceVariant = palette.surfaceSecondary,
        onSurfaceVariant = palette.textSecondary, outline = palette.border, error = palette.missed
    ) else lightColorScheme(
        primary = palette.accent, onPrimary = Color.White, secondary = palette.completed,
        background = palette.background, onBackground = palette.textPrimary, surface = palette.surface,
        onSurface = palette.textPrimary, surfaceVariant = palette.surfaceSecondary,
        onSurfaceVariant = palette.textSecondary, outline = palette.border, error = palette.missed
    )
    CompositionLocalProvider(LocalDaymarkPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography.copy(
                headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
                titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
                bodyLarge = TextStyle(fontSize = 15.sp),
                bodyMedium = TextStyle(fontSize = 14.sp),
                labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp)
            ), content = content
        )
    }
}

fun taskStatusColor(palette: DaymarkPalette, status: com.daymark.app.data.TaskStatus): Color = when (status) {
    com.daymark.app.data.TaskStatus.UPCOMING, com.daymark.app.data.TaskStatus.PLANNED_TODAY -> palette.upcoming
    com.daymark.app.data.TaskStatus.MISSED_PLAN, com.daymark.app.data.TaskStatus.OVERDUE -> palette.missed
    com.daymark.app.data.TaskStatus.COMPLETED -> palette.completed
    com.daymark.app.data.TaskStatus.DUE_TODAY -> palette.dueToday
    com.daymark.app.data.TaskStatus.UNSCHEDULED -> palette.neutral
}
