package com.studentgig.app.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.studentgig.app.R
import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import com.google.gson.reflect.TypeToken
import com.studentgig.app.data.model.Job
import com.studentgig.app.ui.animations.animateCountUp
import com.studentgig.app.ui.animations.shineSweep
import com.studentgig.app.ui.animations.rotatingGlowBorder
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGlow
import com.studentgig.app.ui.theme.GigGradients

// =================================================================================
// 
//   STUDENTGIG UI COMPONENTS
//   All reusable UI components used across multiple screens.
// 
// =================================================================================


// --- Glassmorphism Card -----------------------------------------------------------------------------
// Premium card with subtle border glow and elevated surface.

@Composable
fun GigCard(
    modifier: Modifier = Modifier,
    glowColor: Color = GigColors.Primary.copy(alpha = 0.08f),
    enableShineSweep: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // Subtle periodic shine sweep across the card surface
    val shineMod = if (enableShineSweep) {
        Modifier.shineSweep(durationMs = 6000, color = GigGlow.ShineSweep)
    } else Modifier

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
            modifier = Modifier
                .then(shineMod)
                .padding(16.dp),
            content = content
        )
    }
}


// â”€â”€â”€ Gradient Button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.5f,
        animationSpec = tween(200), label = "btnAlpha"
    )

    // Spring press-and-bounce (dopamine hit on tap)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btnSpringScale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = GigGradients.Primary,
                alpha = animatedAlpha
            )
            .shineSweep(durationMs = 5000, color = Color.White.copy(alpha = 0.08f))
            .clickable(
                enabled = enabled && !isLoading,
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                interactionSource = interactionSource,
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


// â”€â”€â”€ AI Match Score Badge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Glowing badge that shows the AI match percentage.

@Composable
fun GigMatchBadge(score: Int) {
    val color = GigColors.matchScoreColor(score)
    val glowColor = GigColors.matchScoreGlow(score)

    // Count-up animation for the score (0 -> actual value)
    val animatedScore = animateCountUp(targetValue = score, durationMs = 900, delayMs = 300)

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

    // Sparkle icon rotation for extra premium feel
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleRotation"
    )

    Box(
        modifier = Modifier
            .shadow(8.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
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
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer { rotationZ = sparkleRotation }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$animatedScore%",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// â”€â”€â”€ Status Badge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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


// â”€â”€â”€ Urgent Badge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
            Icon(
                imageVector = Icons.Filled.Whatshot,
                contentDescription = null,
                tint = GigColors.Error,
                modifier = Modifier.size(12.dp)
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


// â”€â”€â”€ Skill Chip â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun GigSkillChip(
    skill: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
) {
    val bgColor = if (isHighlighted) GigColors.PrimaryMuted else GigColors.SurfaceHighest
    val textColor = if (isHighlighted) GigColors.PrimaryLight else GigColors.TextSecondary
    val borderColor = if (isHighlighted) GigColors.Primary.copy(alpha = 0.4f) else GigColors.BorderSubtle

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
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


// â”€â”€â”€ Section Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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


// â”€â”€â”€ Premium Job Card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// The main job listing card used across Home and Search screens.

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GigJobCard(
    job: Job,
    isApplying: Boolean,
    onApplyClick: () -> Unit,
    onCardClick: () -> Unit = {},
    isApplied: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring-press bounce on card tap (premium tactile feel)
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardSpring"
    )

    val skills: List<String> = remember(job.skillsRequired) {
        try {
            if (job.skillsRequired.isNullOrBlank()) emptyList()
            else Gson().fromJson(job.skillsRequired, object : TypeToken<List<String>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    GigCard(
        modifier = Modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCardClick()
            },
        glowColor = GigGlow.cardGlow(job.isUrgent)
    ) {
        // â”€â”€â”€ Top Row: Title + Match Badge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
            if ((job.matchScore ?: 0) > 0 || (job.aiScore ?: 0) > 0) {
                Spacer(modifier = Modifier.width(10.dp))
                GigMatchBadge(score = job.aiScore ?: job.matchScore ?: 0)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // â”€â”€â”€ AI Reason â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (!job.aiReason.isNullOrBlank()) {
            Surface(
                color = GigColors.Primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, GigColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Insight", modifier = Modifier.size(14.dp), tint = GigColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = job.aiReason,
                        color = GigColors.PrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // â”€â”€â”€ Description â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€â”€ Info Row: Location + Pay â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = "Location",
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
                    contentDescription = "Pay",
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

        // â”€â”€â”€ Skills Row â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (skills.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
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

        // â”€â”€â”€ Bottom Row: Badges + Apply/Applied â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

            AnimatedContent(
                targetState = isApplied,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "applyButtonMorph"
            ) { applied ->
                if (applied) {
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
                        icon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.widthIn(min = 130.dp)
                    )
                }
            }
        }
    }
}


// â”€â”€â”€ Server Status Indicator â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        isChecking -> "Connecting..."
        isOnline -> "Live"
        else -> "Offline"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .rotatingGlowBorder(
                colors = listOf(dotColor.copy(alpha=0.6f), Color.Transparent, dotColor.copy(alpha=0.2f), Color.Transparent),
                borderWidth = 1.dp,
                durationMs = 2000
            )
            .clip(RoundedCornerShape(20.dp))
            .background(dotColor.copy(alpha = 0.1f))
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


// â”€â”€â”€ Shimmer Effect â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Animated gradient sweep used for skeleton loading placeholders.

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        GigColors.SurfaceHighest.copy(alpha = 0.4f),
        GigColors.SurfaceHighest.copy(alpha = 0.8f),
        GigColors.Primary.copy(alpha = 0.12f),
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


// â”€â”€â”€ Shimmer Job Card (skeleton placeholder) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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


// â”€â”€â”€ Shimmer Loading Screen (multiple shimmer cards) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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


// â”€â”€â”€ Simple Loading State (for non-job screens) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun GigLoadingState(message: String = "Loading...") {
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


// â”€â”€â”€ Empty State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun GigEmptyState(
    icon: ImageVector = Icons.Outlined.WorkOutline,
    title: String = "No jobs found",
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


// â”€â”€â”€ Server Offline State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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


// â”€â”€â”€ Compact Retry Banner (inline, non-blocking) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
// â”€â”€â”€ Confirmation Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Reusable confirmation dialog for destructive actions (logout, reject, etc.).

@Composable
fun GigConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    confirmColor: Color = GigColors.Error,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GigColors.SurfaceHighest,
        titleContentColor = GigColors.TextPrimary,
        textContentColor = GigColors.TextSecondary,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = confirmColor)
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = GigColors.TextMuted)
            ) {
                Text(text = dismissText)
            }
        }
    )
}





