package com.daymark.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import android.animation.ValueAnimator
import com.daymark.app.data.DaymarkSettings
import com.daymark.app.data.DateFormatMode
import com.daymark.app.data.CalendarStatusTone
import com.daymark.app.data.calendarStatusTone

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
    val onAccent: Color,
    val upcoming: Color,
    val missed: Color,
    val completed: Color,
    val dueToday: Color,
    val neutral: Color,
    val selected: Color
)

object DaymarkPalettes {
    val Light = DaymarkPalette(
        background = Color(0xFFF4EFF9), surface = Color(0xFFFDFBFF), surfaceRaised = Color(0xFFE4D9EF),
        surfaceSecondary = Color(0xFFEEE7F5), border = Color(0xFFD6C9E1), textPrimary = Color(0xFF292432),
        textSecondary = Color(0xFF6E6577), textMuted = Color(0xFF73697D), accent = Color(0xFF7852A6), onAccent = Color.White,
        upcoming = Color(0xFF405A83), missed = Color(0xFFA23C41), completed = Color(0xFF2F704B),
        dueToday = Color(0xFF805516), neutral = Color(0xFF6E6577), selected = Color(0xFF7852A6)
    )
    val Dark = DaymarkPalette(
        background = Color(0xFF191C1A), surface = Color(0xFF222723), surfaceRaised = Color(0xFF333A34),
        surfaceSecondary = Color(0xFF2B312C), border = Color(0xFF414A42), textPrimary = Color(0xFFEFF3EE),
        textSecondary = Color(0xFFAEB8B0), textMuted = Color(0xFF859188), accent = Color(0xFFC8FF3D), onAccent = Color(0xFF191C1A),
        upcoming = Color(0xFF9DB5D4), missed = Color(0xFFD98D90), completed = Color(0xFF86B89C),
        dueToday = Color(0xFFD8B776), neutral = Color(0xFF859188), selected = Color(0xFFC8FF3D)
    )
}

object DaymarkMotion {
    const val MICRO = 150
    const val CONTENT = 210
    const val SCREEN = 260
    const val EXPAND = 250
}

object DaymarkMotionSettings { @Volatile var reduceMotion: Boolean = false }
fun motionDuration(requested: Int): Int = if (!DaymarkMotionSettings.reduceMotion && ValueAnimator.areAnimatorsEnabled()) requested else 0

fun <T> daymarkTween(durationMillis: Int) = tween<T>(durationMillis = motionDuration(durationMillis))

val LocalDaymarkPalette = staticCompositionLocalOf { DaymarkPalettes.Light }
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }
val LocalDateFormat = staticCompositionLocalOf { DateFormatMode.WEEKDAY_MONTH_DAY }

@Composable
fun DaymarkTheme(settings: DaymarkSettings, content: @Composable () -> Unit) {
    SideEffect { DaymarkMotionSettings.reduceMotion = settings.reduceMotion }
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (dark) DaymarkPalettes.Dark else DaymarkPalettes.Light
    val scheme = if (dark) darkColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, secondary = palette.completed,
        background = palette.background, onBackground = palette.textPrimary, surface = palette.surface,
        onSurface = palette.textPrimary, surfaceVariant = palette.surfaceSecondary,
        onSurfaceVariant = palette.textSecondary, outline = palette.border, error = palette.missed
    ) else lightColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, secondary = palette.completed,
        background = palette.background, onBackground = palette.textPrimary, surface = palette.surface,
        onSurface = palette.textPrimary, surfaceVariant = palette.surfaceSecondary,
        onSurfaceVariant = palette.textSecondary, outline = palette.border, error = palette.missed
    )
    CompositionLocalProvider(
        LocalDaymarkPalette provides palette,
        LocalReduceMotion provides settings.reduceMotion,
        LocalHapticsEnabled provides settings.hapticsEnabled,
        LocalDateFormat provides settings.dateFormat
    ) {
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

fun taskStatusColor(palette: DaymarkPalette, status: com.daymark.app.data.TaskStatus): Color =
    status.calendarStatusTone().argb?.let { Color(it) } ?: palette.neutral

fun taskStatusTextColor(status: com.daymark.app.data.TaskStatus): Color =
    if (status.calendarStatusTone() == CalendarStatusTone.DEADLINE_TODAY || status.calendarStatusTone() == CalendarStatusTone.COMPLETED) Color(0xFF191C1A) else Color.White
