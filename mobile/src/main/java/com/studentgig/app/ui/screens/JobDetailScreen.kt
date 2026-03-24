package com.studentgig.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studentgig.app.ui.animations.AmbientGlowOrbs
import com.studentgig.app.ui.animations.ConfettiBurst
import com.studentgig.app.ui.animations.cascadeEntrance
import com.studentgig.app.ui.animations.staggeredSlideIn
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGlow
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.JobDetailViewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobDetailScreen(
    jobId: Int,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit = {},
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val job = uiState.job
    val isApplied = jobId in uiState.appliedJobIds
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Track if confetti should trigger
    var showConfetti by remember { mutableStateOf(false) }
    var prevApplied by remember { mutableStateOf(isApplied) }

    LaunchedEffect(isApplied) {
        if (isApplied && !prevApplied) {
            showConfetti = true
        }
        prevApplied = isApplied
    }

    LaunchedEffect(uiState.applyMessage) {
        uiState.applyMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissApplyMessage()
        }
    }

    if (uiState.showLoginSheet) {
        GigLoginBottomSheet(
            onDismiss = { viewModel.dismissLoginSheet() },
            onSuccess = onLoginSuccess
        )
    }

    Scaffold(
        containerColor = GigColors.Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = GigColors.SurfaceElevated,
                    contentColor = GigColors.TextPrimary,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GigColors.SurfaceElevated.copy(alpha = 0.7f))
                                .border(1.dp, GigColors.BorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                "Back",
                                tint = GigColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (job != null) {
                        IconButton(onClick = {
                            val shareText = buildString {
                                append("\uD83D\uDCBC ${job.title}\n")
                                append("📍 ${job.location}\n")
                                append("💰 ₹${job.payAmount.toInt()}\n")
                                if (job.isUrgent) append("🔥 URGENT\n")
                                append("\nFind this job on StudentGig!")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Job"))
                        }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GigColors.SurfaceElevated.copy(alpha = 0.7f))
                                    .border(1.dp, GigColors.BorderSubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Share,
                                    "Share",
                                    tint = GigColors.TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Premium sticky apply bar with glass effect
            if (job != null) {
                Surface(
                    color = GigColors.Surface.copy(alpha = 0.95f),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .drawBehind {
                            // Top gradient border
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        GigColors.Primary.copy(alpha = 0.2f),
                                        GigColors.Accent.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                topLeft = Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = isApplied,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.5f)) togetherWith
                                        fadeOut(animationSpec = tween(200))
                            },
                            label = "applyButtonMorph"
                        ) { applied ->
                            if (applied) {
                                Surface(
                                    color = GigColors.Success.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(64.dp)
                                        .border(1.dp, GigColors.Success.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GigColors.Success, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Application Sent ✨", color = GigColors.Success, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                val haptic = LocalHapticFeedback.current
                                val coroutineScope = rememberCoroutineScope()
                                var currentProgress by remember { mutableStateOf(0f) }
                                var isPressing by remember { mutableStateOf(false) }
                                
                                val animatedProgress by animateFloatAsState(
                                    targetValue = currentProgress,
                                    animationSpec = tween(75, easing = LinearEasing),
                                    label = "holdProgress"
                                )
                                
                                val infiniteTransition = rememberInfiniteTransition(label="pulseGlow")
                                val pulseGlow by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 0.8f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseGlow"
                                )
                                
                                val spinRotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "spin"
                                )

                                val scale by animateFloatAsState(
                                    targetValue = when {
                                        currentProgress >= 1f -> 1.15f
                                        isPressing -> 0.88f
                                        else -> 1f
                                    },
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                                    label = "scale"
                                )

                                val rippleAlpha by animateFloatAsState(
                                    targetValue = if (isPressing && currentProgress < 1f) 1f else 0f,
                                    animationSpec = tween(300),
                                    label = "rippleAlpha"
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        isPressing = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val jobClickJob = coroutineScope.launch {
                                                            var p = currentProgress
                                                            val startTime = System.currentTimeMillis()
                                                            val duration = 750f // Premium fill duration
                                                            var nextHapticTick = 0.15f
                                                            
                                                            while (isPressing && p < 1f && isActive) {
                                                                val elapsed = System.currentTimeMillis() - startTime
                                                                p = (elapsed / duration).coerceIn(0f, 1f)
                                                                
                                                                // Accelerating haptic physical feel
                                                                if (p >= nextHapticTick) {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    nextHapticTick += (0.25f - (p * 0.15f)).coerceAtLeast(0.04f)
                                                                }
                                                                
                                                                currentProgress = p
                                                                delay(16) // Smooth 60fps filling
                                                            }
                                                            if (p >= 1f && isPressing) {
                                                                currentProgress = 1f
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                delay(80)
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                viewModel.onApplyClicked(job.id)
                                                            }
                                                        }
                                                        tryAwaitRelease()
                                                        isPressing = false
                                                        if (currentProgress < 1f && currentProgress > 0f) {
                                                            coroutineScope.launch {
                                                                while (!isPressing && currentProgress > 0f) {
                                                                    currentProgress -= 0.08f
                                                                    if (currentProgress < 0f) currentProgress = 0f
                                                                    delay(16)
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            .drawBehind {
                                                // Ambient rest state breathing
                                                if (currentProgress == 0f) {
                                                    drawCircle( GigColors.Primary.copy(alpha = pulseGlow * 0.35f), radius = size.width / 2f + 16.dp.toPx() * pulseGlow )
                                                }
                                                
                                                val trackWidth = 5.dp.toPx()
                                                val circleRadius = size.width / 2f - trackWidth / 2f
                                                
                                                // Premium tracking background
                                                drawCircle( GigColors.SurfaceHighest, radius = circleRadius, style = Stroke(width = trackWidth) )
                                                
                                                // Emissive Core Fill
                                                if (currentProgress > 0f) {
                                                    drawCircle( 
                                                        color = GigColors.Primary.copy(alpha = 0.15f + (0.15f * currentProgress)),
                                                        radius = circleRadius 
                                                    )
                                                    
                                                    // Dynamic outward pulse ripples
                                                    val rippleRadius = circleRadius + (24.dp.toPx() * currentProgress)
                                                    drawCircle(
                                                        color = GigColors.Primary.copy(alpha = (1f - currentProgress) * 0.4f * rippleAlpha),
                                                        radius = rippleRadius,
                                                        style = Stroke(width = 1.5.dp.toPx())
                                                    )
                                                }
                                                
                                                // Glowing, spinning trace
                                                if (animatedProgress > 0f) {
                                                    rotate(spinRotation) {
                                                        drawArc( 
                                                            brush = Brush.sweepGradient(
                                                                colors = listOf(
                                                                    GigColors.PrimaryLight, GigColors.Primary, 
                                                                    GigColors.Accent, GigColors.Primary, GigColors.PrimaryLight
                                                                )
                                                            ),
                                                            startAngle = -90f, 
                                                            sweepAngle = 360f * animatedProgress, 
                                                            useCenter = false, 
                                                            style = Stroke(width = trackWidth + (3.dp.toPx() * currentProgress), cap = StrokeCap.Round) 
                                                        )
                                                    }
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.size(70.dp).clip(CircleShape).background(if (isPressing) GigColors.Primary.copy(alpha = 0.3f) else GigColors.Primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Fingerprint, 
                                                "Hold to apply", 
                                                tint = if (currentProgress > 0.5f) GigColors.Accent else if (currentProgress > 0f) GigColors.Primary else GigColors.PrimaryLight, 
                                                modifier = Modifier.size(40.dp).graphicsLayer { 
                                                    alpha = 0.8f + (0.2f * currentProgress)
                                                    if (isPressing && currentProgress > 0.3f) {
                                                        val shakeIntensity = 1.5f * currentProgress
                                                        translationX = (Math.random() * 2 - 1).toFloat() * shakeIntensity
                                                        translationY = (Math.random() * 2 - 1).toFloat() * shakeIntensity
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = when {
                                            currentProgress >= 1f -> "Unlocking..."
                                            currentProgress >= 0.75f -> "Almost there..."
                                            currentProgress >= 0.5f -> "Keep holding..."
                                            isPressing -> "Processing..."
                                            else -> "Hold to Apply"
                                        },
                                        color = GigColors.TextPrimary.copy(alpha = if (isPressing) 0.6f else 1f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {
            if (job == null) {
                GigEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Job not found",
                    subtitle = "This job may have been removed"
                )
                return@Scaffold
            }

            val skills: List<String> = remember(job.skillsRequired) {
                try {
                    if (job.skillsRequired.isNullOrBlank()) emptyList()
                    else Gson().fromJson(job.skillsRequired, object : TypeToken<List<String>>() {}.type)
                } catch (_: Exception) { emptyList() }
            }

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .verticalScroll(scrollState)
            ) {
                // ─── Hero Section ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Parallax depth effect
                            translationY = scrollState.value * 0.4f
                            alpha = 1f - (scrollState.value / 600f)
                        }
                        .background(GigGradients.HeroAmbient)
                ) {
                    // Ambient Glow Orbs
                    AmbientGlowOrbs(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        orbCount = 2,
                        primaryColor = GigGlow.OrbPrimary,
                        accentColor = GigGlow.OrbAccent,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = padding.calculateTopPadding())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Badges row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (job.isUrgent) {
                                GigUrgentBadge()
                            }
                            if (job.matchScore != null && job.matchScore > 0) {
                                GigMatchBadge(score = job.matchScore)
                            }
                        }

                        if (job.isUrgent || (job.matchScore != null && job.matchScore > 0)) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Title
                        Text(
                            text = job.title,
                            color = GigColors.TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.75).sp,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Employer Info
                        val employerName = job.companyName ?: job.employerName ?: "Employer"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Business,
                                contentDescription = null,
                                tint = GigColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Posted by $employerName",
                                color = GigColors.TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick info chips with staggered entrance
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            var chipIndex = 0
                            
                            Box(modifier = Modifier.staggeredSlideIn(index = chipIndex++, baseDelayMs = 100)) {
                                InfoChip(
                                    icon = Icons.Outlined.LocationOn,
                                    text = job.location,
                                    color = GigColors.Info
                                )
                            }
                            Box(modifier = Modifier.staggeredSlideIn(index = chipIndex++, baseDelayMs = 100)) {
                                InfoChip(
                                    icon = Icons.Outlined.CurrencyRupee,
                                    text = "₹${job.payAmount.toInt()}",
                                    color = GigColors.Success
                                )
                            }
                            job.category?.let { category ->
                                Box(modifier = Modifier.staggeredSlideIn(index = chipIndex++, baseDelayMs = 100)) {
                                    InfoChip(
                                        icon = Icons.Outlined.WorkOutline,
                                        text = category,
                                        color = GigColors.Accent
                                    )
                                }
                            }
                            Box(modifier = Modifier.staggeredSlideIn(index = chipIndex++, baseDelayMs = 100)) {
                                InfoChip(
                                    icon = Icons.Outlined.AccessTime,
                                    text = job.jobType.replaceFirstChar { it.uppercase() },
                                    color = GigColors.Primary
                                )
                            }
                            if (job.applicantCount != null && job.applicantCount > 0) {
                                Box(modifier = Modifier.staggeredSlideIn(index = chipIndex++, baseDelayMs = 100)) {
                                    InfoChip(
                                        icon = Icons.Outlined.PeopleOutline,
                                        text = "${job.applicantCount} Applicants",
                                        color = GigColors.Warning
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Description Card ───────────────────────────────────────────
                if (!job.description.isNullOrBlank()) {
                    GigCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .cascadeEntrance(0)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GigColors.Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = GigColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "About this Job",
                                color = GigColors.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = job.description,
                            color = GigColors.TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ─── Skills Required Card ───────────────────────────────────────
                if (skills.isNotEmpty()) {
                    GigCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .cascadeEntrance(1)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GigColors.Accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = GigColors.Accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Skills Required",
                                color = GigColors.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            skills.forEachIndexed { index, skill ->
                                GigSkillChip(
                                    skill = skill,
                                    isHighlighted = true,
                                    modifier = Modifier.staggeredSlideIn(index, baseDelayMs = 60)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ─── Phase 1: Dedicated Schedule & Timings Card ───────────────────
                if (job.jobDate != null || job.startTime != null || job.duration != null) {
                    GigCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .cascadeEntrance(2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GigColors.Warning.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = GigColors.Warning,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Schedule & Timings",
                                color = GigColors.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (job.jobDate != null) {
                            DetailRow(
                                icon = Icons.Outlined.Event,
                                label = "Date",
                                value = job.jobDate
                            )
                        }
                        if (job.startTime != null && job.endTime != null) {
                            DetailRow(
                                icon = Icons.Outlined.Schedule,
                                label = "Time",
                                value = "${job.startTime} - ${job.endTime}"
                            )
                        } else if (job.startTime != null) {
                            DetailRow(
                                icon = Icons.Outlined.Schedule,
                                label = "Starts At",
                                value = job.startTime
                            )
                        }
                        if (job.duration != null) {
                            DetailRow(
                                icon = Icons.Outlined.HourglassEmpty,
                                label = "Duration",
                                value = job.duration
                            )
                        }
                        if (job.address != null) {
                            DetailRow(
                                icon = Icons.Outlined.Place,
                                label = "Address",
                                value = job.address
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ─── Job Details Card ───────────────────────────────────────────
                GigCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .cascadeEntrance(2)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GigColors.Info.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = GigColors.Info,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Details",
                            color = GigColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(icon = Icons.Outlined.LocationOn, label = "Location", value = job.location)
                    DetailRow(icon = Icons.Outlined.CurrencyRupee, label = "Pay", value = "₹${job.payAmount.toInt()}")
                    DetailRow(
                        icon = Icons.Outlined.Schedule,
                        label = "Posted",
                        value = job.createdAt?.take(10) ?: "Recently"
                    )
                    DetailRow(
                        icon = Icons.Outlined.Bolt,
                        label = "Priority",
                        value = if (job.isUrgent) "Urgent" else "Normal"
                    )

                    // (Schedule info moved to dedicated card above)

                    if (job.matchScore != null && job.matchScore > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = GigColors.BorderSubtle)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val matchColor = GigColors.matchScoreColor(job.matchScore)
                        val explanation = uiState.matchExplanation
                        val isExpandedMatch = remember { mutableStateOf(false) }
                        
                        // AI Match insight with enhanced design
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GigColors.matchScoreGlow(job.matchScore))
                                .border(1.dp, matchColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .clickable { isExpandedMatch.value = !isExpandedMatch.value }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = matchColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI Match Score: ${job.matchScore}%",
                                        color = matchColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = explanation?.explanation ?: when {
                                            job.matchScore >= 80 -> "Excellent match! Your skills align well."
                                            job.matchScore >= 50 -> "Good match. Consider applying!"
                                            else -> "Partial match. You can still apply."
                                        },
                                        color = GigColors.TextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                                if (explanation != null) {
                                    Icon(
                                        if (isExpandedMatch.value) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = GigColors.TextMuted
                                    )
                                }
                            }
                            
                            if (explanation != null && isExpandedMatch.value) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = matchColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (explanation.matchedSkills.isNotEmpty()) {
                                    Text("✅ Matched Skills", color = GigColors.Success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(explanation.matchedSkills.joinToString(", "), color = GigColors.TextPrimary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                if (explanation.missingSkills.isNotEmpty()) {
                                    Text("❌ Missing Skills", color = GigColors.Error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(explanation.missingSkills.joinToString(", "), color = GigColors.TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                        
                        if (uiState.isLoadingMatch) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = GigColors.Accent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Space for bottom bar
            }

            // ─── Confetti Celebration on Apply ──────────────────────────────
            ConfettiBurst(
                trigger = showConfetti,
                colors = GigGlow.ConfettiColors,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


// ─── Info Chip (location / pay in hero) ─────────────────────────────────────────

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


// ─── Detail Row ─────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(GigColors.SurfaceHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GigColors.TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = GigColors.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            color = GigColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
