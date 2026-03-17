package com.studentgig.app.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studentgig.app.ui.theme.GigColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🎨 The "Living UI" Engine — World-Class Animation Primitives
 * Purely visual modifiers and composables that handle premium micro-interactions.
 * 
 * Rules: 
 * - Only handle pure graphics and animations. No business logic.
 * - Always use FastOutSlowInEasing or springs for organic movement.
 * - All animations use graphicsLayer (GPU-composited, zero layout passes).
 */


// ═══════════════════════════════════════════════════════════════════════════════════
//  1. SPRING PRESS — Apple-like satisfying "squish" on tap
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.springPress(): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            clip = false
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  2. AMBIENT SHIMMER — Slowly passing highlight giving life to buttons/cards
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.ambientShimmer(
    durationMillis: Int = 4000,
    delayMillis: Int = 2000,
    shimmerColor: Color = Color.White.copy(alpha = 0.08f)
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val phase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing, delayMillis = delayMillis),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    this.drawWithContent {
        drawContent()
        
        val gradient = Brush.linearGradient(
            colors = listOf(Color.Transparent, shimmerColor, Color.Transparent),
            start = Offset(size.width * phase, -size.height),
            end = Offset(size.width * phase + 100f, size.height * 2)
        )
        
        drawRect(brush = gradient, blendMode = BlendMode.Lighten)
    }
}

fun Modifier.shineSweep(
    durationMs: Int = 4000,
    delayMs: Int = 2000,
    color: Color = Color.White.copy(alpha = 0.08f)
): Modifier = this.then(ambientShimmer(durationMs, delayMs, color))


// ═══════════════════════════════════════════════════════════════════════════════════
//  3. COUNT-UP ANIMATION — Dopamine hit on match scores
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedCounter(
    targetCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    var count by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(targetCount) {
        delay(400)
        count = targetCount
    }
    
    val currentCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "counter"
    )
    
    content(currentCount)
}

@Composable
fun animateCountUp(targetValue: Int, durationMs: Int = 1200, delayMs: Int = 400): Int {
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetValue) {
        delay(delayMs.toLong())
        count = targetValue
    }
    val currentCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "counter"
    )
    return currentCount
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  4. AMBIENT GLOW BACKGROUND — Ethereal living background for hero sections
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AmbientGlowBackground(
    modifier: Modifier = Modifier,
    color1: Color = GigColors.Primary.copy(alpha = 0.15f),
    color2: Color = GigColors.Accent.copy(alpha = 0.10f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambientBg")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val radius1 = size.width * 0.7f * scale
        val radius2 = size.width * 0.9f * (2f - scale)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.3f),
                radius = radius1
            ),
            radius = radius1,
            center = Offset(size.width * 0.2f, size.height * 0.3f)
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.7f),
                radius = radius2
            ),
            radius = radius2,
            center = Offset(size.width * 0.8f, size.height * 0.7f)
        )
    }
}

@Composable
fun AmbientGlowOrbs(
    modifier: Modifier = Modifier,
    orbCount: Int = 3,
    primaryColor: Color = GigColors.Primary.copy(alpha = 0.15f),
    accentColor: Color = GigColors.Accent.copy(alpha = 0.10f)
) {
    AmbientGlowBackground(modifier, primaryColor, accentColor)
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  5. CASCADE ENTRANCE — Staggered item reveal with spring physics
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.cascadeEntrance(
    index: Int,
    baseDelayMs: Int = 50,
    maxDelayMs: Int = 350
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val delayTime = (index * baseDelayMs).coerceAtMost(maxDelayMs).toLong()
        delay(delayTime)
        isVisible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "cascadeAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cascadeOffset"
    )
    this.graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  6. FLOATING EFFECT — Gentle idle hover for interactive elements
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.floatingEffect(amplitude: Dp = 2.dp, durationMs: Int = 4000): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -amplitude.value,
        targetValue = amplitude.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    this.graphicsLayer {
        translationY = offsetY
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  7. PULSE GLOW — Breathing scale for glowing elements
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.PulseGlow(
    color: Color = GigColors.Primary,
    scaleMax: Float = 1.1f,
    durationMs: Int = 1200
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = scaleMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  8. ROTATING GLOW BORDER — Animated gradient border that orbits the element
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.rotatingGlowBorder(
    colors: List<Color>,
    borderWidth: Dp = 2.dp,
    durationMs: Int = 3000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "rotatingBorder")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    this.drawWithContent {
        drawContent()
        val cx = size.width / 2f
        val cy = size.height / 2f
        
        val brush = Brush.sweepGradient(
            colors = colors,
            center = Offset(cx, cy)
        )
        
        val strokeWidth = borderWidth.toPx()
        val rectSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)
        
        with(drawContext.canvas) {
            save()
            rotate(angle, cx, cy)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = rectSize,
                cornerRadius = CornerRadius(rectSize.height / 2f),
                style = Stroke(width = strokeWidth)
            )
            restore()
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  9. ✨ NEW — CONFETTI BURST — Particle celebration on success actions
// ═══════════════════════════════════════════════════════════════════════════════════

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiBurst(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        GigColors.Primary,
        GigColors.Accent,
        GigColors.Success,
        GigColors.Warning,
        Color(0xFF38BDF8),
        Color(0xFFFBBF24)
    )
) {
    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }
    var progress by remember { mutableFloatStateOf(0f) }
    var active by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "confettiProgress"
    )

    LaunchedEffect(trigger) {
        if (trigger) {
            particles = List(30) {
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                val speed = Random.nextFloat() * 600f + 200f
                ConfettiParticle(
                    x = 0.5f,
                    y = 0.5f,
                    velocityX = cos(angle) * speed,
                    velocityY = sin(angle) * speed - 400f,
                    size = Random.nextFloat() * 8f + 4f,
                    color = colors[Random.nextInt(colors.size)],
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 720f - 360f
                )
            }
            progress = 0f
            active = true
            progress = 1f
            delay(1500)
            active = false
        }
    }

    if (active) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = animatedProgress
            particles.forEach { p ->
                val gravity = 800f
                val px = size.width * p.x + p.velocityX * t
                val py = size.height * p.y + p.velocityY * t + 0.5f * gravity * t * t
                val alpha = (1f - t).coerceIn(0f, 1f)

                if (px in 0f..size.width && py in -100f..size.height + 100f) {
                    drawContext.canvas.save()
                    drawContext.canvas.translate(px, py)
                    drawContext.canvas.rotate(p.rotation + p.rotationSpeed * t)
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(-p.size / 2, -p.size / 2),
                        size = Size(p.size, p.size * 0.6f)
                    )
                    drawContext.canvas.restore()
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  10. ✨ NEW — ORBITAL LOADER — Premium loading spinner with orbiting dots
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun OrbitalLoader(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    color: Color = GigColors.Primary,
    accentColor: Color = GigColors.Accent,
    orbRadius: Float = 40f,
    dotRadius: Float = 5f,
    durationMs: Int = 1800
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbitalPulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)

        // Core glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
                center = center,
                radius = orbRadius * 1.5f
            ),
            radius = orbRadius * 1.5f * pulseScale,
            center = center
        )

        // Orbiting dots
        for (i in 0 until dotCount) {
            val angle = rotation + (i * 360f / dotCount)
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val x = center.x + cos(rad) * orbRadius
            val y = center.y + sin(rad) * orbRadius
            val t = i.toFloat() / dotCount
            val dotColor = lerp(color, accentColor, t)
            val currentDotRadius = dotRadius * (1f + 0.3f * sin(Math.toRadians((rotation * 2 + i * 120).toDouble()).toFloat()))

            // Glow around dot
            drawCircle(
                color = dotColor.copy(alpha = 0.3f),
                radius = currentDotRadius * 2.5f,
                center = Offset(x, y)
            )
            // Solid dot
            drawCircle(
                color = dotColor,
                radius = currentDotRadius,
                center = Offset(x, y)
            )
        }

        // Central dot
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = dotRadius * 0.6f * pulseScale,
            center = center
        )
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t
    )
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  11. ✨ NEW — GLASSMORPHISM MODIFIER — Frosted glass effect for premium surfaces
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.glassSurface(
    glowColor: Color = GigColors.Primary.copy(alpha = 0.06f),
    borderAlpha: Float = 0.15f,
    cornerRadius: Dp = 20.dp
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "glassShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing, delayMillis = 4000),
            repeatMode = RepeatMode.Restart
        ),
        label = "glassShimmerOffset"
    )

    this.drawBehind {
        // Subtle top-left highlight (glass refraction)
        val highlightBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f * shimmerOffset.coerceIn(0f, 1f)),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        drawRoundRect(
            brush = highlightBrush,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            size = size
        )

        // Border glow
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = borderAlpha * 0.3f),
                Color.Transparent,
                glowColor.copy(alpha = borderAlpha * 0.5f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        drawRoundRect(
            brush = borderBrush,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = 1.dp.toPx()),
            size = size
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  12. ✨ NEW — GRADIENT ACCENT BAR — Animated left-edge accent for cards
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.gradientAccentBar(
    colors: List<Color> = listOf(GigColors.Primary, GigColors.Accent),
    barWidth: Dp = 3.dp,
    cornerRadius: Dp = 2.dp
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "accentBar")
    val shift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accentShift"
    )

    this.drawBehind {
        val shiftedColors = listOf(
            lerp(colors[0], colors.getOrElse(1) { colors[0] }, shift),
            lerp(colors.getOrElse(1) { colors[0] }, colors[0], shift)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(shiftedColors),
            topLeft = Offset(0f, size.height * 0.1f),
            size = Size(barWidth.toPx(), size.height * 0.8f),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  13. ✨ NEW — STAGGERED ENTRANCE — Items fly in one by one with spring physics
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.staggeredSlideIn(
    index: Int,
    baseDelayMs: Int = 80,
    fromRight: Boolean = false
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * baseDelayMs).toLong())
        isVisible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    val offsetX by animateFloatAsState(
        targetValue = if (isVisible) 0f else if (fromRight) 80f else -80f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggerOffset"
    )
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "staggerScale"
    )
    this.graphicsLayer {
        this.alpha = alpha
        translationX = offsetX
        scaleX = scale
        scaleY = scale
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  14. ✨ NEW — BREATHING GLOW — Subtle scale + alpha pulse for badges
// ═══════════════════════════════════════════════════════════════════════════════════

fun Modifier.breathingGlow(
    color: Color = GigColors.Primary,
    maxAlpha: Float = 0.25f,
    durationMs: Int = 2000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "breathingGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = maxAlpha * 0.3f,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathAlpha"
    )

    this.drawBehind {
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension
            ),
            size = size,
            cornerRadius = CornerRadius(size.height / 2)
        )
    }
}
