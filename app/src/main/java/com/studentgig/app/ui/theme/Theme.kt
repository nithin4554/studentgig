package com.studentgig.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════════════
//  STUDENTGIG DESIGN SYSTEM — Premium Theme
//  Dark-first design with vibrant accent gradients.
//  All colors reference GigColors from Colors.kt
// ═══════════════════════════════════════════════════════════════════════════════════

// ─── Dark Color Scheme ──────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = GigColors.Primary,
    onPrimary = GigColors.TextOnPrimary,
    primaryContainer = GigColors.PrimaryDark,
    onPrimaryContainer = Color(0xFFE0E7FF),

    secondary = GigColors.Accent,
    onSecondary = GigColors.TextOnPrimary,
    secondaryContainer = Color(0xFF3A2F5C),
    onSecondaryContainer = Color(0xFFD8B4FE),

    tertiary = GigColors.Success,
    onTertiary = GigColors.TextOnPrimary,

    background = GigColors.Background,
    surface = GigColors.Surface,
    surfaceVariant = GigColors.SurfaceElevated,
    surfaceContainerHigh = GigColors.SurfaceHighest,

    onBackground = GigColors.TextPrimary,
    onSurface = GigColors.TextPrimary,
    onSurfaceVariant = GigColors.TextSecondary,

    error = GigColors.Error,
    onError = GigColors.TextOnPrimary,

    outline = GigColors.Border,
    outlineVariant = GigColors.BorderSubtle,
)

// ─── Light Color Scheme (future use) ────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = GigColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    secondary = GigColors.Accent,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    error = GigColors.Error,
)

// ─── Typography ─────────────────────────────────────────────────────────────────
// Clean, modern type scale with tight letter-spacing for a premium feel.

private val StudentGigTypography = Typography(
    // Hero / Display
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.75).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.25).sp
    ),
    // Titles
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Body
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    // Labels
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    ),
)

// ─── Shapes ─────────────────────────────────────────────────────────────────────
// Generous rounding for a soft, modern feel.

private val StudentGigShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// ─── Theme ──────────────────────────────────────────────────────────────────────

@Composable
fun StudentGigTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudentGigTypography,
        shapes = StudentGigShapes,
        content = content
    )
}
