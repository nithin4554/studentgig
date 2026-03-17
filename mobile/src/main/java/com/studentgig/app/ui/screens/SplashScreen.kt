package com.studentgig.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // Phase: 0=Particles expand, 1=Particles implode, 2=Logo bloom, 3=Tagline reveal
    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        phase = 0
        delay(1000)   // Particles orbit outward
        phase = 1
        delay(700)    // Particles implode to center
        phase = 2
        delay(500)    // Logo blooms
        phase = 3
        delay(1800)   // Tagline + hold
        onSplashComplete()
    }

    // ─── Orbital ring rotation ──────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splashAmbient")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseRotation"
    )

    // ─── Orb distance animation ─────────────────────────────────────────
    val orbDistance by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 200f
            1 -> 0f
            else -> 0f
        },
        animationSpec = when (phase) {
            0 -> tween(1000, easing = FastOutSlowInEasing)
            else -> spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)
        },
        label = "orbDistance"
    )

    // ─── Background pulse (brightens on implosion) ──────────────────────
    val bgPulse by animateFloatAsState(
        targetValue = when {
            phase >= 2 -> 0.9f
            phase == 1 -> 0.5f
            else -> 0.15f
        },
        animationSpec = tween(800),
        label = "bgPulse"
    )

    // ─── Ring scales (two concentric rings) ─────────────────────────────
    val ring1Scale by animateFloatAsState(
        targetValue = if (phase >= 2) 2.5f else 0.3f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow),
        label = "ring1"
    )
    val ring1Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 0f else 0.3f,
        animationSpec = tween(1200),
        label = "ring1Alpha"
    )
    val ring2Scale by animateFloatAsState(
        targetValue = if (phase >= 2) 3.5f else 0.5f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessVeryLow),
        label = "ring2"
    )
    val ring2Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 0f else 0.2f,
        animationSpec = tween(1500),
        label = "ring2Alpha"
    )

    // ─── Logo reveal ────────────────────────────────────────────────────
    val logoScale by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(400),
        label = "logoAlpha"
    )

    // ─── Tagline reveal ─────────────────────────────────────────────────
    val tagAlpha by animateFloatAsState(
        targetValue = if (phase >= 3) 1f else 0f,
        animationSpec = tween(600),
        label = "tagAlpha"
    )
    val tagOffsetY by animateFloatAsState(
        targetValue = if (phase >= 3) 0f else 15f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "tagOffset"
    )

    // ─── Particle system ────────────────────────────────────────────────
    val particles = remember {
        List(20) {
            SplashParticle(
                baseAngle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 0.5f + 0.7f,
                distanceMultiplier = Random.nextFloat() * 0.4f + 0.8f,
                size = Random.nextFloat() * 3f + 2f,
                color = listOf(
                    GigColors.Primary,
                    GigColors.Accent,
                    GigColors.Info,
                    Color(0xFFA78BFA),
                    Color(0xFF38BDF8)
                )[Random.nextInt(5)]
            )
        }
    }

    // ─── Render ─────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GigColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // Deep background glow
        Box(
            modifier = Modifier
                .size(500.dp)
                .alpha(bgPulse)
                .blur(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GigColors.Primary.copy(alpha = 0.7f),
                            GigColors.Accent.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Expanding rings (shockwave effect on implosion)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            // Ring 1
            drawCircle(
                color = GigColors.Primary.copy(alpha = ring1Alpha),
                radius = 100f * ring1Scale,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )

            // Ring 2
            drawCircle(
                color = GigColors.Accent.copy(alpha = ring2Alpha),
                radius = 80f * ring2Scale,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }

        // Orbital particles + mini glow orbs
        if (phase < 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val numOrbs = 3

                // 3 main glowing orbs
                for (i in 0 until numOrbs) {
                    val angle = rotation + (i * (360f / numOrbs))
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val x = center.x + cos(rad) * orbDistance
                    val y = center.y + sin(rad) * orbDistance

                    val orbColor = when (i) {
                        0 -> GigColors.Primary
                        1 -> GigColors.Accent
                        else -> GigColors.Info
                    }

                    // Outer glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                orbColor.copy(alpha = 0.8f),
                                orbColor.copy(alpha = 0.0f)
                            ),
                            center = Offset(x, y),
                            radius = 140f
                        ),
                        radius = 140f,
                        center = Offset(x, y)
                    )

                    // Core point
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 5f,
                        center = Offset(x, y)
                    )
                }

                // Tiny particles orbiting in reverse
                particles.forEach { p ->
                    val angle = reverseRotation * p.speed + p.baseAngle
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val dist = orbDistance * p.distanceMultiplier * 1.3f
                    val x = center.x + cos(rad) * dist
                    val y = center.y + sin(rad) * dist

                    drawCircle(
                        color = p.color.copy(alpha = 0.7f),
                        radius = p.size,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // ─── Logo Text Reveal ───────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha = logoAlpha
                }
        ) {
            Text(
                text = "StudentGig",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = GigGradients.Primary
                ),
                letterSpacing = (-1.5).sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Subtle gradient underline
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .graphicsLayer { alpha = logoAlpha * 0.7f }
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GigColors.Primary,
                                GigColors.Accent,
                                Color.Transparent
                            )
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ELEVATE YOUR HUSTLE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GigColors.TextSecondary,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = tagAlpha
                        translationY = tagOffsetY
                    }
            )
        }
    }
}

private data class SplashParticle(
    val baseAngle: Float,
    val speed: Float,
    val distanceMultiplier: Float,
    val size: Float,
    val color: Color
)
