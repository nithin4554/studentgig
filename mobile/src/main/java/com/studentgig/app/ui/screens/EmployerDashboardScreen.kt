package com.studentgig.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.data.model.ApplicationDetailResponse
import com.studentgig.app.data.model.AIApplicantResponse
import com.studentgig.app.ui.animations.cascadeEntrance
import com.studentgig.app.ui.animations.breathingGlow
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.EmployerViewModel


// ═══════════════════════════════════════════════════════════════════════════════════
//  EMPLOYER DASHBOARD SCREEN — Manage posted gigs, applicants, and lifecycle
// ═══════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerDashboardScreen(
    viewModel: EmployerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Refresh when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Snackbar for action feedback
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissActionMessage()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (state.actionSuccess) GigColors.Success
                    else GigColors.Error,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = GigColors.Background
    ) { padding ->

        if (!state.isLoggedIn) {
            // Not logged in
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = GigColors.TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Log in to manage your jobs",
                        style = MaterialTheme.typography.titleMedium,
                        color = GigColors.TextSecondary
                    )
                }
            }
            return@Scaffold
        }

        val filteredApps = viewModel.getFilteredApplications()

        // Group applications by job
        val appsByJob = filteredApps.groupBy { it.jobId }

        val pullState = rememberPullToRefreshState()

        PullToRefreshBox(
            state = pullState,
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
            indicator = {
                Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = state.isLoading,
                    state = pullState,
                    containerColor = GigColors.SurfaceElevated,
                    color = GigColors.Primary,
                )
            }
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ─── Header ──────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GigGradients.HeaderGlow)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Text(
                            "My Posted Jobs",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GigColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Manage applicants & track job progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GigColors.TextSecondary
                        )
                    }
                }
            }

            // ─── Stats Row ───────────────────────────────────────────────────
            item {
                val total = state.applications.size
                val pending = state.applications.count { it.status == "pending" }
                val active = state.applications.count {
                    it.status in listOf("accepted", "checked_in", "in_progress", "work_done")
                }
                val completed = state.applications.count {
                    it.status in listOf("confirmed", "paid")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppStatChip(
                        count = total,
                        label = "Total",
                        color = GigColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    AppStatChip(
                        count = pending,
                        label = "Pending",
                        color = GigColors.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    AppStatChip(
                        count = active,
                        label = "Active",
                        color = GigColors.Primary,
                        modifier = Modifier.weight(1f)
                    )
                    AppStatChip(
                        count = completed,
                        label = "Done",
                        color = GigColors.Success,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─── Filter Chips ────────────────────────────────────────────────
            item {
                EmployerFilterChipRow(
                    selectedFilter = state.selectedFilter,
                    onFilterSelected = { viewModel.setFilter(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ─── Loading State ───────────────────────────────────────────────
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GigColors.Primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // ─── Empty State ─────────────────────────────────────────────────
            // ─── Empty State ─────────────────────────────────────────────────
            if (!state.isLoading && state.myJobs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Business,
                                contentDescription = null,
                                tint = GigColors.TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No jobs posted yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = GigColors.TextSecondary
                            )
                            Text(
                                "Post jobs from the Home tab!",
                                style = MaterialTheme.typography.bodySmall,
                                color = GigColors.TextMuted
                            )
                        }
                    }
                }
            }

            val jobsToShow = if (state.selectedFilter == "all") {
                state.myJobs
            } else {
                state.myJobs.filter { job ->
                    filteredApps.any { it.jobId == job.id }
                }
            }

            // ─── Application Cards (grouped by job) ──────────────────────────
            jobsToShow.forEachIndexed { groupIndex, job ->
                val apps = filteredApps.filter { it.jobId == job.id }

                item {
                    // Job header
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = if (groupIndex > 0) 16.dp else 8.dp, bottom = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(GigColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Work,
                                    contentDescription = null,
                                    tint = GigColors.PrimaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = job.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GigColors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${job.location} • ${job.applicantCount ?: 0} applicant${if (job.applicantCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            
                            // Status badge for the job itself
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GigColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = job.status.replaceFirstChar { it.uppercaseChar() },
                                    color = GigColors.Primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            
                            if (apps.isNotEmpty() && state.selectedFilter == "all") {
                                Spacer(Modifier.width(8.dp))
                                if (state.isLoadingRanks.contains(job.id)) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GigColors.Accent, strokeWidth = 2.dp)
                                } else if (!state.aiRankings.keys.any { id -> apps.any { app -> app.id == id } }) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GigColors.Accent.copy(alpha = 0.15f),
                                        modifier = Modifier.clickable { viewModel.loadAIRankingsForJob(job.id) }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(10.dp), tint = GigColors.Accent)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "AI Rank",
                                                color = GigColors.Accent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (apps.isEmpty() && state.selectedFilter == "all") {
                    item {
                        Text(
                            text = "No applicants yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GigColors.TextMuted,
                            modifier = Modifier.padding(start = 54.dp, top = 4.dp, bottom = 12.dp)
                        )
                    }
                }

                // Individual applicant cards
                val sortedApps = apps.sortedByDescending { state.aiRankings[it.id]?.aiRankScore ?: 0 }
                itemsIndexed(sortedApps) { index, app ->
                    EmployerApplicantCard(
                        app = app,
                        aiRanking = state.aiRankings[app.id],
                        isActionLoading = state.actionLoading == app.id,
                        onAccept = { viewModel.acceptApplication(app.id) },
                        onReject = { viewModel.rejectApplication(app.id) },
                        onConfirmArrival = { viewModel.confirmArrival(app.id) },
                        onConfirmCompletion = { viewModel.confirmCompletion(app.id) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .cascadeEntrance(groupIndex * 3 + index)
                    )
                }
            }
        }
        } // PullToRefreshBox
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  EMPLOYER APPLICANT CARD
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun EmployerApplicantCard(
    app: ApplicationDetailResponse,
    aiRanking: AIApplicantResponse? = null,
    isActionLoading: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onConfirmArrival: () -> Unit,
    onConfirmCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = GigColors.statusColor(app.status)
    val statusBg = GigColors.statusBackground(app.status)
    val statusLabel = GigColors.statusLabel(app.status)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = statusColor.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GigColors.SurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            // ─── Applicant info + Status ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GigColors.Primary, GigColors.Accent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Applicant #${app.userId}",
                            style = MaterialTheme.typography.titleSmall,
                            color = GigColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Applied for ₹${String.format("%.0f", app.jobPayAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GigColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // ─── AI Ranking Insight ──────────────────────────────────────────
            if (aiRanking != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = GigColors.Accent.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, GigColors.Accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, "AI", tint = GigColors.Accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Match Score: ${aiRanking.aiRankScore ?: 0}%", color = GigColors.Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (aiRanking.aiBadges.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(color = GigColors.Warning.copy(alpha=0.2f), shape=RoundedCornerShape(6.dp)) {
                                    Text("🏆 ${aiRanking.aiBadges.joinToString(", ")}", color = GigColors.Warning.copy(alpha=0.9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Timeline ────────────────────────────────────────────────────
            ApplicationTimeline(
                status = app.status,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // ─── Employer Action Buttons ─────────────────────────────────────
            when (app.status.lowercase()) {
                "pending" -> {
                    // Accept / Reject buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (!isActionLoading) onReject() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GigColors.Error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, GigColors.Error.copy(alpha = 0.4f)
                            ),
                            enabled = !isActionLoading
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { if (!isActionLoading) onAccept() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GigColors.Success
                            ),
                            enabled = !isActionLoading,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                "accepted" -> {
                    // Waiting for student to check in
                    WaitingIndicator(
                        message = "Waiting for student to check in...",
                        color = GigColors.Info
                    )
                }
                "checked_in" -> {
                    // Employer: Confirm arrival
                    ActionButton(
                        text = "Confirm Student Arrived ✔️",
                        icon = Icons.Filled.HowToReg,
                        color = GigColors.Primary,
                        isLoading = isActionLoading,
                        onClick = onConfirmArrival
                    )
                }
                "in_progress" -> {
                    // Work in progress
                    WaitingIndicator(
                        message = "Student is working...",
                        color = GigColors.Primary
                    )
                }
                "work_done" -> {
                    // Employer: Confirm completion
                    ActionButton(
                        text = "Confirm Work Quality ✨",
                        icon = Icons.Filled.Verified,
                        color = GigColors.Success,
                        isLoading = isActionLoading,
                        onClick = onConfirmCompletion
                    )
                }
                "confirmed" -> {
                    WaitingIndicator(
                        message = "Confirmed! Awaiting student payment collection.",
                        color = GigColors.Success
                    )
                }
                "paid" -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = GigColors.Success.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = null,
                                tint = GigColors.SuccessLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Complete — Payment Released 🎉",
                                style = MaterialTheme.typography.bodySmall,
                                color = GigColors.SuccessLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "rejected" -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = GigColors.Error.copy(alpha = 0.06f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Rejected",
                                style = MaterialTheme.typography.bodySmall,
                                color = GigColors.Error.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun WaitingIndicator(
    message: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PulsingDot(color = color)
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerFilterChipRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        "all" to "All",
        "pending" to "Pending",
        "accepted" to "Accepted",
        "checked_in" to "Checked In",
        "in_progress" to "Working",
        "work_done" to "Done",
        "confirmed" to "Confirmed",
        "paid" to "Paid"
    )

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (key, label) ->
            val isSelected = selectedFilter == key
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(key) },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GigColors.Primary.copy(alpha = 0.2f),
                    selectedLabelColor = GigColors.PrimaryLight,
                    containerColor = GigColors.SurfaceElevated,
                    labelColor = GigColors.TextSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) GigColors.Primary.copy(alpha = 0.4f)
                    else GigColors.BorderSubtle
                )
            )
        }
    }
}
