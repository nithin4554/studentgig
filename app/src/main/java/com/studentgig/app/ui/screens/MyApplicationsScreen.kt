package com.studentgig.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.data.model.ApplicationDetailResponse
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.ApplicationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    viewModel: ApplicationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = GigColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Header ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GigGradients.HeaderGlow)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "My Applications",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GigColors.TextPrimary,
                        letterSpacing = (-0.75).sp
                    )
                    Text(
                        text = "Track your job applications",
                        fontSize = 13.sp,
                        color = GigColors.TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // ─── Content ────────────────────────────────────────────────────
            when {
                !uiState.isLoggedIn -> {
                    GigEmptyState(
                        icon = Icons.Outlined.LockOpen,
                        title = "Login Required",
                        subtitle = "Sign in from the Home tab to view your applications"
                    )
                }
                uiState.isLoading -> {
                    GigShimmerLoading(cardCount = 3)
                }
                uiState.errorMessage != null -> {
                    GigOfflineState(
                        errorMessage = uiState.errorMessage ?: "Something went wrong",
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.applications.isEmpty() -> {
                    GigEmptyState(
                        icon = Icons.Outlined.Description,
                        title = "No applications yet",
                        subtitle = "Apply for gigs from the Home or Search tab"
                    )
                }
                else -> {
                    // Stats row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val pending = uiState.applications.count { it.status.lowercase() == "pending" }
                        val accepted = uiState.applications.count { it.status.lowercase() == "accepted" }
                        val total = uiState.applications.size

                        AppStatChip(
                            count = total,
                            label = "Total",
                            color = GigColors.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        AppStatChip(
                            count = pending,
                            label = "Pending",
                            color = GigColors.Warning,
                            modifier = Modifier.weight(1f)
                        )
                        AppStatChip(
                            count = accepted,
                            label = "Accepted",
                            color = GigColors.Success,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val pullState = rememberPullToRefreshState()

                    PullToRefreshBox(
                        state = pullState,
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            Indicator(
                                modifier = Modifier.align(Alignment.TopCenter),
                                isRefreshing = uiState.isLoading,
                                state = pullState,
                                containerColor = GigColors.SurfaceElevated,
                                color = GigColors.Primary,
                            )
                        }
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.applications, key = { it.id }) { app ->
                                ApplicationCard(app = app)
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}


// ─── Stat Chip ──────────────────────────────────────────────────────────────────

@Composable
private fun AppStatChip(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = "$count",
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = color.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ─── Application Card ───────────────────────────────────────────────────────────

@Composable
private fun ApplicationCard(app: ApplicationDetailResponse) {
    val statusColor = GigColors.statusColor(app.status)
    val statusStep = when (app.status.lowercase()) {
        "pending" -> 1
        "accepted", "rejected" -> 3
        else -> 2
    }

    GigCard(
        glowColor = statusColor.copy(alpha = 0.05f)
    ) {
        // Title + Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = app.jobTitle,
                color = GigColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            GigStatusBadge(status = app.status)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = GigColors.TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = app.jobLocation,
                    color = GigColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CurrencyRupee,
                    contentDescription = null,
                    tint = GigColors.Success,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "₹${app.jobPayAmount.toInt()}",
                    color = GigColors.SuccessLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (app.jobIsUrgent) {
                GigUrgentBadge()
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── Status Timeline ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GigColors.SurfaceHighest, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step 1: Applied
            TimelineStep(
                label = "Applied",
                icon = Icons.Filled.CheckCircle,
                isActive = statusStep >= 1,
                color = statusColor
            )

            // Connector
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(
                        if (statusStep >= 2) statusColor.copy(alpha = 0.5f)
                        else GigColors.Border
                    )
            )

            // Step 2: Under Review
            TimelineStep(
                label = "Review",
                icon = Icons.Filled.HourglassTop,
                isActive = statusStep >= 2,
                color = statusColor
            )

            // Connector
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(
                        if (statusStep >= 3) statusColor.copy(alpha = 0.5f)
                        else GigColors.Border
                    )
            )

            // Step 3: Decision
            TimelineStep(
                label = when (app.status.lowercase()) {
                    "accepted" -> "Accepted"
                    "rejected" -> "Rejected"
                    else -> "Decision"
                },
                icon = when (app.status.lowercase()) {
                    "accepted" -> Icons.Filled.Verified
                    "rejected" -> Icons.Filled.Cancel
                    else -> Icons.Outlined.PendingActions
                },
                isActive = statusStep >= 3,
                color = statusColor
            )
        }

        // Applied date
        if (app.appliedAt != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = GigColors.TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Applied ${app.appliedAt.take(10)}",
                    color = GigColors.TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}


// ─── Timeline Step ─────────────────────────────────────────────────────────────

@Composable
private fun TimelineStep(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) color else GigColors.TextMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) color else GigColors.TextMuted.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
