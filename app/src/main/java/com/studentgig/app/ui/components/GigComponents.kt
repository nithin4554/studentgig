package com.studentgig.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.studentgig.app.R
import com.google.gson.reflect.TypeToken
import com.studentgig.app.data.model.Job
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients

// ═══════════════════════════════════════════════════════════════════════════════════
//  PREMIUM SHARED COMPONENTS
//  All reusable UI components used across multiple screens.
// ═══════════════════════════════════════════════════════════════════════════════════


// ─── Glassmorphism Card ─────────────────────────────────────────────────────────
// Premium card with subtle border glow and elevated surface.

@Composable
fun GigCard(
    modifier: Modifier = Modifier,
    glowColor: Color = GigColors.Primary.copy(alpha = 0.08f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GigColors.BorderLight,
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = GigColors.SurfaceElevated
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}


// ─── Gradient Button ────────────────────────────────────────────────────────────

@Composable
fun GigGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
) {
    val haptic = LocalHapticFeedback.current
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.5f,
        animationSpec = tween(200), label = "btnAlpha"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = GigGradients.Primary,
                alpha = animatedAlpha
            )
            .clickable(
                enabled = enabled && !isLoading,
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}


// ─── AI Match Score Badge ───────────────────────────────────────────────────────
// Glowing badge that shows the AI match percentage.

@Composable
fun GigMatchBadge(score: Int) {
    val color = GigColors.matchScoreColor(score)
    val glowColor = GigColors.matchScoreGlow(score)

    val infiniteTransition = rememberInfiniteTransition(label = "matchGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "matchGlowAlpha"
    )

    Box(
        modifier = Modifier
            .shadow(6.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .background(
                color = color.copy(alpha = glowAlpha * 0.2f),
                shape = CircleShape
            )
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "AI Match",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$score%",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ─── Status Badge ───────────────────────────────────────────────────────────────
// Used in My Applications screen for pending/accepted/rejected.

@Composable
fun GigStatusBadge(status: String) {
    val color = GigColors.statusColor(status)
    val bgColor = GigColors.statusBackground(status)
    val label = status.replaceFirstChar { it.uppercaseChar() }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}


// ─── Urgent Badge ───────────────────────────────────────────────────────────────

@Composable
fun GigUrgentBadge() {
    Surface(
        color = GigColors.ErrorMuted,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, GigColors.Error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "🔥",
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "URGENT",
                color = GigColors.Error,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}


// ─── Skill Chip ─────────────────────────────────────────────────────────────────

@Composable
fun GigSkillChip(
    skill: String,
    isHighlighted: Boolean = false,
) {
    val bgColor = if (isHighlighted) GigColors.PrimaryMuted else GigColors.SurfaceHighest
    val textColor = if (isHighlighted) GigColors.PrimaryLight else GigColors.TextSecondary
    val borderColor = if (isHighlighted) GigColors.Primary.copy(alpha = 0.4f) else GigColors.BorderSubtle

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = skill,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}


// ─── Section Header ─────────────────────────────────────────────────────────────

@Composable
fun GigSectionHeader(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = GigColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = GigColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (action != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = action,
                    color = GigColors.PrimaryLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// ─── Premium Job Card ───────────────────────────────────────────────────────────
// The main job listing card used across Home and Search screens.

@Composable
fun GigJobCard(
    job: Job,
    isApplying: Boolean,
    onApplyClick: () -> Unit,
    onCardClick: () -> Unit = {},
    isApplied: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val skills: List<String> = remember(job.skillsRequired) {
        try {
            if (job.skillsRequired.isNullOrBlank()) emptyList()
            else Gson().fromJson(job.skillsRequired, object : TypeToken<List<String>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    GigCard(
        modifier = Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCardClick()
        },
        glowColor = if (job.isUrgent) GigColors.Error.copy(alpha = 0.06f)
                     else GigColors.Primary.copy(alpha = 0.05f)
    ) {
        // ─── Top Row: Title + Match Badge ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.title,
                    color = GigColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.2).sp
                )
            }
            if (job.matchScore != null && job.matchScore > 0) {
                Spacer(modifier = Modifier.width(10.dp))
                GigMatchBadge(score = job.matchScore)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Description ────────────────────────────────────────────────
        if (!job.description.isNullOrBlank()) {
            Text(
                text = job.description,
                color = GigColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ─── Info Row: Location + Pay ───────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = GigColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = job.location,
                    color = GigColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            // Pay
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CurrencyRupee,
                    contentDescription = null,
                    tint = GigColors.Success,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "₹${job.payAmount.toInt()}",
                    color = GigColors.SuccessLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ─── Skills Row ─────────────────────────────────────────────────
        if (skills.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                skills.take(4).forEach { skill ->
                    GigSkillChip(skill = skill)
                }
                if (skills.size > 4) {
                    GigSkillChip(skill = "+${skills.size - 4}")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ─── Bottom Row: Badges + Apply/Applied ─────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (job.isUrgent) {
                GigUrgentBadge()
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (isApplied) {
                // "Applied" badge
                Surface(
                    color = GigColors.Success.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .widthIn(min = 130.dp)
                        .height(40.dp)
                        .border(1.dp, GigColors.Success.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = GigColors.Success,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Applied",
                            color = GigColors.Success,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                GigGradientButton(
                    text = "Apply Now",
                    onClick = onApplyClick,
                    isLoading = isApplying,
                    icon = Icons.Filled.Send,
                    modifier = Modifier.widthIn(min = 130.dp)
                )
            }
        }
    }
}


// ─── Server Status Indicator ────────────────────────────────────────────────────

@Composable
fun GigServerDot(isOnline: Boolean, isChecking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "serverPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dotColor by animateColorAsState(
        targetValue = when {
            isChecking -> GigColors.Warning
            isOnline -> GigColors.Success
            else -> GigColors.Error
        },
        animationSpec = tween(500),
        label = "dotColor"
    )

    val label = when {
        isChecking -> "Connecting…"
        isOnline -> "Live"
        else -> "Offline"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(dotColor.copy(alpha = 0.1f))
            .border(1.dp, dotColor.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    if (isChecking) {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                }
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = dotColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}


// ─── Shimmer Effect ─────────────────────────────────────────────────────────────
// Animated gradient sweep used for skeleton loading placeholders.

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        GigColors.SurfaceHighest.copy(alpha = 0.4f),
        GigColors.SurfaceHighest.copy(alpha = 0.8f),
        GigColors.SurfaceHighest.copy(alpha = 0.4f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    widthFraction: Float = 1f,
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}


// ─── Shimmer Job Card (skeleton placeholder) ────────────────────────────────────

@Composable
fun ShimmerJobCard() {
    GigCard {
        // Title placeholder
        ShimmerBox(height = 18.dp, widthFraction = 0.7f)
        Spacer(modifier = Modifier.height(10.dp))
        // Description lines
        ShimmerBox(height = 12.dp, widthFraction = 1f)
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBox(height = 12.dp, widthFraction = 0.85f)
        Spacer(modifier = Modifier.height(14.dp))
        // Info row (location + pay)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ShimmerBox(height = 14.dp, widthFraction = 0.25f, modifier = Modifier.weight(1f))
            ShimmerBox(height = 14.dp, widthFraction = 0.2f, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Skill chips row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush())
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        // Bottom row (button placeholder)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(1.dp))
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimmerBrush())
            )
        }
    }
}


// ─── Shimmer Loading Screen (multiple shimmer cards) ────────────────────────────

@Composable
fun GigShimmerLoading(cardCount: Int = 4) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(cardCount) {
            ShimmerJobCard()
        }
    }
}


// ─── Simple Loading State (for non-job screens) ─────────────────────────────────

@Composable
fun GigLoadingState(message: String = "Loading…") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = GigColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = GigColors.TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ─── Empty State ────────────────────────────────────────────────────────────────

@Composable
fun GigEmptyState(
    icon: ImageVector = Icons.Outlined.WorkOutline,
    title: String = "No gigs found",
    subtitle: String = "Check back later for new opportunities",
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(GigColors.PrimaryMuted, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GigColors.Primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                color = GigColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = GigColors.TextMuted,
                fontSize = 13.sp
            )
        }
    }
}


// ─── Server Offline State ───────────────────────────────────────────────────────

@Composable
fun GigOfflineState(
    errorMessage: String = "Server Offline",
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated icon
            val infiniteTransition = rememberInfiniteTransition(label = "offlinePulse")
            val iconAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offlineAlpha"
            )

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(GigColors.ErrorMuted, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = "Offline",
                    tint = GigColors.Error.copy(alpha = iconAlpha),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Server Offline",
                color = GigColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = GigColors.TextMuted,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            GigGradientButton(
                text = "Retry Connection",
                onClick = onRetry,
                icon = Icons.Filled.Refresh,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }
}


// ─── Compact Retry Banner (inline, non-blocking) ─────────────────────────────────

@Composable
fun GigRetryBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = GigColors.ErrorMuted,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = GigColors.Error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = GigColors.Error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = GigColors.Error
                )
            ) {
                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ─── Login Bottom Sheet ─────────────────────────────────────────────────────────
// Shared login sheet used in HomeScreen and SearchScreen.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GigLoginBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (phone: String, name: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val isPhoneValid = phone.length == 10 && phone.all { it.isDigit() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GigColors.SurfaceElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GigColors.TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(GigColors.PrimaryMuted, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Login,
                        contentDescription = null,
                        tint = GigColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Welcome to StudentGig",
                        color = GigColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Sign in to apply for gigs",
                        color = GigColors.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Phone Field
            Text(
                text = "Phone Number",
                color = GigColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    // Only accept digits, max 10
                    val digits = it.filter { c -> c.isDigit() }.take(10)
                    phone = digits
                    phoneError = when {
                        digits.isEmpty() -> null
                        digits.length < 10 -> "Enter a 10-digit phone number"
                        !digits.first().let { d -> d == '6' || d == '7' || d == '8' || d == '9' } ->
                            "Phone must start with 6, 7, 8 or 9"
                        else -> null
                    }
                },
                placeholder = { Text("10-digit mobile number") },
                leadingIcon = {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = GigColors.Primary)
                },
                isError = phoneError != null,
                supportingText = if (phoneError != null) {
                    { Text(phoneError!!, color = GigColors.Error, fontSize = 11.sp) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (phoneError != null) GigColors.Error else GigColors.Primary,
                    unfocusedBorderColor = if (phoneError != null) GigColors.Error.copy(alpha = 0.5f) else GigColors.Border,
                    cursorColor = GigColors.Primary,
                    focusedTextColor = GigColors.TextPrimary,
                    unfocusedTextColor = GigColors.TextPrimary,
                    focusedContainerColor = GigColors.SurfaceHighest,
                    unfocusedContainerColor = GigColors.SurfaceHighest,
                    focusedPlaceholderColor = GigColors.TextMuted,
                    unfocusedPlaceholderColor = GigColors.TextMuted,
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name Field
            Text(
                text = "Your Name (optional)",
                color = GigColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter your name") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = GigColors.Accent)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isPhoneValid && phoneError == null) {
                            onLogin(phone, name.ifBlank { null })
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GigColors.Accent,
                    unfocusedBorderColor = GigColors.Border,
                    cursorColor = GigColors.Accent,
                    focusedTextColor = GigColors.TextPrimary,
                    unfocusedTextColor = GigColors.TextPrimary,
                    focusedContainerColor = GigColors.SurfaceHighest,
                    unfocusedContainerColor = GigColors.SurfaceHighest,
                    focusedPlaceholderColor = GigColors.TextMuted,
                    unfocusedPlaceholderColor = GigColors.TextMuted,
                )
            )

            // Server error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GigColors.ErrorMuted, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = GigColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = GigColors.Error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            GigGradientButton(
                text = "Continue",
                onClick = {
                    if (isPhoneValid && phoneError == null) {
                        onLogin(phone, name.ifBlank { null })
                    } else {
                        phoneError = "Enter a valid 10-digit phone number"
                    }
                },
                enabled = isPhoneValid && phoneError == null,
                isLoading = isLoading,
                icon = Icons.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy note
            Text(
                text = "By continuing, you agree to our Terms of Service",
                color = GigColors.TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
