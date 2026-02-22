package com.studentgig.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.JobDetailViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobDetailScreen(
    jobId: Int,
    onBack: () -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val job = uiState.job
    val isApplied = jobId in uiState.appliedJobIds
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.applyMessage) {
        uiState.applyMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissApplyMessage()
        }
    }

    if (uiState.showLoginSheet) {
        GigLoginBottomSheet(
            isLoading = uiState.isLoggingIn,
            errorMessage = uiState.loginError,
            onLogin = { phone, name -> viewModel.onLoginSubmit(phone, name) },
            onDismiss = { viewModel.dismissLoginSheet() }
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
                        Icon(
                            Icons.Filled.ArrowBack,
                            "Back",
                            tint = GigColors.TextPrimary
                        )
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
                                append("\nFind this gig on StudentGig!")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Gig"))
                        }) {
                            Icon(
                                Icons.Outlined.Share,
                                "Share",
                                tint = GigColors.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Sticky apply button at the bottom
            if (job != null) {
                Surface(
                    color = GigColors.Surface,
                    shadowElevation = 16.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pay", color = GigColors.TextMuted, fontSize = 11.sp)
                            Text(
                                text = "₹${job.payAmount.toInt()}",
                                color = GigColors.SuccessLight,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        if (isApplied) {
                            Surface(
                                color = GigColors.Success.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .widthIn(min = 160.dp)
                                    .height(48.dp)
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Applied",
                                        color = GigColors.Success,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        } else {
                            GigGradientButton(
                                text = "Apply Now",
                                onClick = { viewModel.onApplyClicked(job.id) },
                                isLoading = uiState.isApplying,
                                icon = Icons.Filled.Send,
                                modifier = Modifier.widthIn(min = 160.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (job == null) {
            GigEmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "Job not found",
                subtitle = "This gig may have been removed"
            )
            return@Scaffold
        }

        val skills: List<String> = remember(job.skillsRequired) {
            try {
                if (job.skillsRequired.isNullOrBlank()) emptyList()
                else Gson().fromJson(job.skillsRequired, object : TypeToken<List<String>>() {}.type)
            } catch (_: Exception) { emptyList() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ─── Hero Section ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GigGradients.HeaderGlow)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Column {
                    // Badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.75).sp,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick info chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InfoChip(
                            icon = Icons.Outlined.LocationOn,
                            text = job.location,
                            color = GigColors.Info
                        )
                        InfoChip(
                            icon = Icons.Outlined.CurrencyRupee,
                            text = "₹${job.payAmount.toInt()}",
                            color = GigColors.Success
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Description Card ───────────────────────────────────────────
            if (!job.description.isNullOrBlank()) {
                GigCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = GigColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "About this Gig",
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
                GigCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = GigColors.Accent,
                            modifier = Modifier.size(20.dp)
                        )
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
                        skills.forEach { skill ->
                            GigSkillChip(skill = skill, isHighlighted = true)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ─── Job Details Card ───────────────────────────────────────────
            GigCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = GigColors.Info,
                        modifier = Modifier.size(20.dp)
                    )
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

                if (job.matchScore != null && job.matchScore > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GigColors.BorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))
                    // AI Match insight
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                GigColors.matchScoreGlow(job.matchScore),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = GigColors.matchScoreColor(job.matchScore),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI Match Score: ${job.matchScore}%",
                                color = GigColors.matchScoreColor(job.matchScore),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    job.matchScore >= 80 -> "Excellent match! Your skills align well."
                                    job.matchScore >= 50 -> "Good match. Consider applying!"
                                    else -> "Partial match. You can still apply."
                                },
                                color = GigColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Space for bottom bar
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GigColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
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
