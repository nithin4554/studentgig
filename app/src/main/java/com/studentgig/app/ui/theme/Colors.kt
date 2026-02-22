package com.studentgig.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * StudentGig Design System — Premium Color Palette
 *
 * All screens MUST use these tokens instead of defining local colors.
 * Organized by: Background → Primary → Semantic → Text → Borders → Gradients
 */
object GigColors {

    // ─── Background Layers (deepest → highest) ─────────────────────────────
    val Background       = Color(0xFF0B0A14)   // Deep space
    val Surface          = Color(0xFF13111F)   // Main surface
    val SurfaceElevated  = Color(0xFF1A1730)   // Cards, sheets
    val SurfaceHighest   = Color(0xFF221F36)   // Raised elements
    val Overlay          = Color(0xFF2A264A)   // Popups, modals

    // ─── Primary (Indigo) ───────────────────────────────────────────────────
    val Primary          = Color(0xFF6366F1)
    val PrimaryLight     = Color(0xFF818CF8)
    val PrimaryDark      = Color(0xFF4F46E5)
    val PrimaryMuted     = Color(0xFF6366F1).copy(alpha = 0.15f)

    // ─── Accent (Violet) ────────────────────────────────────────────────────
    val Accent           = Color(0xFF8B5CF6)
    val AccentLight      = Color(0xFFA78BFA)
    val AccentMuted      = Color(0xFF8B5CF6).copy(alpha = 0.15f)

    // ─── Semantic Colors ────────────────────────────────────────────────────
    val Success          = Color(0xFF10B981)
    val SuccessLight     = Color(0xFF34D399)
    val SuccessMuted     = Color(0xFF10B981).copy(alpha = 0.15f)

    val Warning          = Color(0xFFF59E0B)
    val WarningLight     = Color(0xFFFBBF24)
    val WarningMuted     = Color(0xFFF59E0B).copy(alpha = 0.15f)

    val Error            = Color(0xFFF43F5E)
    val ErrorLight       = Color(0xFFFB7185)
    val ErrorMuted       = Color(0xFFF43F5E).copy(alpha = 0.15f)

    val Info             = Color(0xFF06B6D4)
    val InfoLight        = Color(0xFF22D3EE)
    val InfoMuted        = Color(0xFF06B6D4).copy(alpha = 0.15f)

    // ─── Text Hierarchy ─────────────────────────────────────────────────────
    val TextPrimary      = Color(0xFFF1F5F9)
    val TextSecondary    = Color(0xFF94A3B8)
    val TextMuted        = Color(0xFF64748B)
    val TextOnPrimary    = Color(0xFFFFFFFF)

    // ─── Borders ────────────────────────────────────────────────────────────
    val Border           = Color(0xFF2D2855)
    val BorderLight      = Color(0xFF3730A3).copy(alpha = 0.3f)
    val BorderSubtle     = Color(0xFFFFFFFF).copy(alpha = 0.06f)

    // ─── Special: Match Score Tiers ─────────────────────────────────────────
    fun matchScoreColor(score: Int): Color = when {
        score >= 80 -> Success
        score >= 50 -> Warning
        else -> Error
    }

    fun matchScoreGlow(score: Int): Color = when {
        score >= 80 -> Success.copy(alpha = 0.25f)
        score >= 50 -> Warning.copy(alpha = 0.25f)
        else -> Error.copy(alpha = 0.25f)
    }

    // ─── Status Colors ──────────────────────────────────────────────────────
    fun statusColor(status: String): Color = when (status.lowercase()) {
        "accepted" -> Success
        "rejected" -> Error
        else -> Warning   // pending
    }

    fun statusBackground(status: String): Color = when (status.lowercase()) {
        "accepted" -> SuccessMuted
        "rejected" -> ErrorMuted
        else -> WarningMuted
    }
}

/**
 * Pre-built gradient brushes for consistent use across the app.
 */
object GigGradients {

    // ─── Primary gradient (buttons, hero banners) ───────────────────────────
    val Primary = Brush.horizontalGradient(
        colors = listOf(GigColors.Primary, GigColors.Accent)
    )

    val PrimaryVertical = Brush.verticalGradient(
        colors = listOf(GigColors.Primary, GigColors.Accent)
    )

    // ─── Subtle header/background wash ──────────────────────────────────────
    val HeaderFade = Brush.verticalGradient(
        colors = listOf(
            GigColors.Primary.copy(alpha = 0.12f),
            Color.Transparent
        )
    )

    val HeaderGlow = Brush.verticalGradient(
        colors = listOf(
            GigColors.Accent.copy(alpha = 0.08f),
            GigColors.Primary.copy(alpha = 0.05f),
            Color.Transparent
        )
    )

    // ─── Card glow effects ──────────────────────────────────────────────────
    val CardBorder = Brush.verticalGradient(
        colors = listOf(
            GigColors.Primary.copy(alpha = 0.3f),
            GigColors.Accent.copy(alpha = 0.1f),
            Color.Transparent
        )
    )

    val UrgentGlow = Brush.horizontalGradient(
        colors = listOf(GigColors.Error, GigColors.Warning)
    )

    // ─── Background ambient glow ────────────────────────────────────────────
    val BackgroundAmbient = Brush.radialGradient(
        colors = listOf(
            GigColors.Primary.copy(alpha = 0.06f),
            Color.Transparent
        )
    )
}
