package com.studentgig.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.data.model.ApplicationDetailResponse
import com.studentgig.app.ui.animations.cascadeEntrance
import com.studentgig.app.ui.animations.breathingGlow
import com.studentgig.app.ui.animations.staggeredSlideIn
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.ApplicationsViewModel
import com.studentgig.app.ui.viewmodel.ActivityRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    viewModel: ApplicationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filteredApps = viewModel.getFilteredApplications()

    var appToRate by remember { androidx.compose.runtime.mutableStateOf<com.studentgig.app.data.model.ApplicationDetailResponse?>(null) }

    // Refresh on screen visit — lifecycle-aware to catch login from other tabs
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Snackbar for action messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissActionMessage()
        }
    }

    // Login sheet (triggered from Activity tab when not logged in)
    if (state.showLoginSheet) {
        GigLoginBottomSheet(
            onDismiss = { viewModel.dismissLoginSheet() },
            onSuccess = { viewModel.dismissLoginSheet() }
        )
    }

    Scaffold(
        containerColor = GigColors.Background,
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
        }
    ) { padding ->
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
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ─── Header with Role Toggle ─────────────────────────────────────
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
                            text = "Activity",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GigColors.TextPrimary,
                            letterSpacing = (-0.75).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.activeRole == ActivityRole.STUDENT)
                                "Track your applications & earnings"
                            else "Manage applicants & track progress",
                            fontSize = 13.sp,
                            color = GigColors.TextSecondary
                        )
                        // Gradient underline
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            GigColors.Primary,
                                            GigColors.Accent,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // ─── Role Toggle (only when user has BOTH roles) ────
                        if (state.isLoggedIn && state.hasStudentData && state.hasPosterData) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GigColors.SurfaceElevated)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RoleToggleButton(
                                    text = "My Applications",
                                    selected = state.activeRole == ActivityRole.STUDENT,
                                    onClick = { viewModel.setActiveRole(ActivityRole.STUDENT) },
                                    modifier = Modifier.weight(1f)
                                )
                                RoleToggleButton(
                                    text = "My Posts",
                                    selected = state.activeRole == ActivityRole.POSTER,
                                    onClick = { viewModel.setActiveRole(ActivityRole.POSTER) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else if (state.isLoggedIn && state.hasPosterData && !state.hasStudentData) {
                            // Only poster — show a subtle indicator
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "📋 Showing your posted jobs",
                                fontSize = 12.sp,
                                color = GigColors.TextMuted
                            )
                        }
                    }
                }
            }

            // ─── Earnings Card ───────────────────────────────────────────────
            if (state.isLoggedIn && state.earnings != null) {
                item {
                    val earnings = state.earnings!!
                    EarningsCard(
                        totalEarned = earnings.totalEarned,
                        pendingPayment = earnings.pendingPayment,
                        gigsCompleted = earnings.gigsCompleted,
                        gigsInProgress = earnings.gigsInProgress,
                        recentPayments = earnings.recentPayments,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .cascadeEntrance(0)
                    )
                }
            }

            // ─── Stats Row ───────────────────────────────────────────────────
            if (state.applications.isNotEmpty()) {
                item {
                    val pending = state.applications.count { it.status.lowercase() == "pending" }
                    val active = state.applications.count { it.status.lowercase() in listOf("accepted", "in_progress") }
                    val completed = state.applications.count { it.status.lowercase() in listOf("completed", "paid") }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .cascadeEntrance(1),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppStatChip(pending, "Pending", GigColors.Warning, Modifier.weight(1f))
                        AppStatChip(active, "Active", GigColors.Primary, Modifier.weight(1f))
                        AppStatChip(completed, "Done", GigColors.Success, Modifier.weight(1f))
                    }
                }
            }

            // ─── Filter Chips ────────────────────────────────────────────────
            if (state.applications.isNotEmpty()) {
                item {
                    FilterChipRow(
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = { viewModel.setFilter(it) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .cascadeEntrance(2)
                    )
                }
            }



            // ─── Loading ─────────────────────────────────────────────────────
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GigColors.Primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // ─── Error ───────────────────────────────────────────────────────
            if (state.errorMessage != null && !state.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = GigColors.Error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.errorMessage!!,
                            color = GigColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GigColors.Primary
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            // ─── Not logged in ───────────────────────────────────────────────
            if (!state.isLoggedIn) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = GigColors.TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Sign in to track your jobs",
                            style = MaterialTheme.typography.titleMedium,
                            color = GigColors.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "View applications, earnings & progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GigColors.TextSecondary
                        )
                        Spacer(Modifier.height(24.dp))
                        GigGradientButton(
                            text = "Sign In to Track Jobs",
                            onClick = { viewModel.showLogin() },
                            icon = Icons.Filled.Login,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }
            }

            // ─── Empty State ─────────────────────────────────────────────────
            if (state.isLoggedIn && filteredApps.isEmpty() && !state.isLoading && state.errorMessage == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (state.activeRole == ActivityRole.POSTER) Icons.Outlined.PostAdd
                            else Icons.Outlined.WorkOutline,
                            contentDescription = null,
                            tint = GigColors.TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (state.activeRole == ActivityRole.POSTER) {
                                if (state.selectedFilter == "all") "No applicants yet"
                                else "No ${state.selectedFilter.replace("_", " ")} applicants"
                            } else {
                                if (state.selectedFilter == "all") "No applications yet"
                                else "No ${state.selectedFilter.replace("_", " ")} jobs"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = GigColors.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.activeRole == ActivityRole.POSTER)
                                "Post jobs and students will apply!"
                            else "Browse jobs and start applying!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GigColors.TextSecondary
                        )
                    }
                }
            }

            // ─── Application Cards ───────────────────────────────────────────
            itemsIndexed(
                items = filteredApps,
                key = { _, app -> app.id }
            ) { index, app ->
                LifecycleApplicationCard(
                    app = app,
                    isActionLoading = state.actionLoading == app.id,
                    currentUserId = viewModel.getUserId(),
                    viewingAsEmployer = state.activeRole == ActivityRole.POSTER,
                    onCheckIn = { viewModel.checkIn(app.id) },
                    onStartWork = { viewModel.startWork(app.id) },
                    onCompleteWork = { viewModel.completeWork(app.id) },
                    onConfirmCompletion = { viewModel.confirmCompletion(app.id) },
                    onConfirmPayment = { viewModel.confirmPayment(app.id) },
                    onAccept = { viewModel.acceptApplication(app.id) },
                    onReject = { viewModel.rejectApplication(app.id) },
                    onConfirmArrival = { viewModel.confirmArrival(app.id) },
                    onRate = { appToRate = app },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .cascadeEntrance(index + 4)
                )
            }
        }
        } // PullToRefreshBox
    }

    if (appToRate != null) {
        RatingDialog(
            app = appToRate!!,
            viewingAsEmployer = state.activeRole == ActivityRole.POSTER,
            isLoading = state.actionLoading == appToRate!!.id,
            onDismiss = { appToRate = null },
            onSubmit = { ratedUserId, score, review ->
                viewModel.rateApplication(appToRate!!.id, ratedUserId, score, review)
                appToRate = null
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════
//  RATING DIALOG
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun RatingDialog(
    app: com.studentgig.app.data.model.ApplicationDetailResponse,
    viewingAsEmployer: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ratedUserId: Int, score: Int, review: String?) -> Unit
) {
    var score by remember { androidx.compose.runtime.mutableStateOf(5) }
    var review by remember { androidx.compose.runtime.mutableStateOf("") }
    
    val targetName = if (viewingAsEmployer) "Student" else "Employer"
    val targetId = if (viewingAsEmployer) app.userId else app.jobEmployerId ?: 0

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = GigColors.SurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Rate your experience", fontWeight = FontWeight.Bold, color = GigColors.TextPrimary)
        },
        text = {
            Column {
                Text("How was your experience with the $targetName?", style = MaterialTheme.typography.bodyMedium, color = GigColors.TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..5) {
                        IconButton(onClick = { score = i }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "$i star",
                                tint = if (i <= score) GigColors.Warning else GigColors.TextMuted.copy(alpha=0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    placeholder = { Text("Write a brief review (optional)", style = MaterialTheme.typography.bodyMedium, color = GigColors.TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GigColors.Primary,
                        unfocusedBorderColor = GigColors.SurfaceHighest,
                        focusedContainerColor = GigColors.Background,
                        unfocusedContainerColor = GigColors.Background
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(targetId, score, review.takeIf { it.isNotBlank() }) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GigColors.Primary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Rating", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = GigColors.TextSecondary)
            }
        }
    )
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  EARNINGS CARD
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun EarningsCard(
    totalEarned: Double,
    pendingPayment: Double,
    gigsCompleted: Int,
    gigsInProgress: Int,
    recentPayments: List<com.studentgig.app.data.model.PaymentRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            GigColors.Primary.copy(alpha = 0.15f),
                            GigColors.Accent.copy(alpha = 0.10f),
                            GigColors.Success.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            GigColors.Primary.copy(alpha = 0.3f),
                            GigColors.Success.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GigColors.Success.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = GigColors.SuccessLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Earnings",
                            style = MaterialTheme.typography.titleMedium,
                            color = GigColors.TextPrimary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Total earned — big number
                Text(
                    text = "₹${String.format("%.0f", totalEarned)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = GigColors.SuccessLight
                )
                Text(
                    text = "Total Earned",
                    style = MaterialTheme.typography.bodySmall,
                    color = GigColors.TextSecondary
                )

                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EarningStatItem(
                        value = "₹${String.format("%.0f", pendingPayment)}",
                        label = "Pending",
                        color = GigColors.Warning
                    )
                    EarningStatItem(
                        value = "$gigsCompleted",
                        label = "Completed",
                        color = GigColors.Success
                    )
                    EarningStatItem(
                        value = "$gigsInProgress",
                        label = "In Progress",
                        color = GigColors.Primary
                    )
                }
                
                // Recent Payments Section
                if (recentPayments.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Recent Payments",
                        style = MaterialTheme.typography.titleSmall,
                        color = GigColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    recentPayments.take(3).forEach { payment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = payment.jobTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GigColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "From ${payment.employerName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GigColors.TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "+₹${String.format("%.0f", payment.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = GigColors.SuccessLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (payment != recentPayments.take(3).last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = GigColors.BorderSubtle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EarningStatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GigColors.TextMuted
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  FILTER CHIP ROW
// ═══════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipRow(
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
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) GigColors.Primary.copy(alpha = 0.4f)
                    else GigColors.BorderSubtle
                )
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  ROLE TOGGLE BUTTON — Switch between Student / Poster views
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun RoleToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) GigColors.Primary else Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else GigColors.TextSecondary
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  LIFECYCLE APPLICATION CARD — The star of the show
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun LifecycleApplicationCard(
    app: ApplicationDetailResponse,
    isActionLoading: Boolean,
    currentUserId: Int,
    viewingAsEmployer: Boolean = false,
    onCheckIn: () -> Unit,
    onStartWork: () -> Unit,
    onCompleteWork: () -> Unit,
    onConfirmCompletion: () -> Unit,
    onConfirmPayment: () -> Unit,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onConfirmArrival: () -> Unit = {},
    onRate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusColor = GigColors.statusColor(app.status)
    val statusBg = GigColors.statusBackground(app.status)
    val statusLabel = GigColors.statusLabel(app.status)
    // Use the tab toggle role, not auto-detection
    val isEmployer = viewingAsEmployer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = statusColor.copy(alpha = 0.15f),
                spotColor = statusColor.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GigColors.SurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            // ─── Top: Title + Status Badge ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.jobTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = GigColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = GigColors.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = app.jobLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = GigColors.TextMuted
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Pay Amount ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.CurrencyRupee,
                    contentDescription = null,
                    tint = GigColors.SuccessLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "₹${String.format("%.0f", app.jobPayAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = GigColors.SuccessLight,
                    fontWeight = FontWeight.Bold
                )
                if (app.jobIsUrgent) {
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GigColors.ErrorMuted
                    ) {
                        Text(
                            "⚡ URGENT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            color = GigColors.Error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── Schedule Info (Phase 1) ──────────────────────────────────────
            if (!app.jobDate.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = GigColors.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            append(app.jobDate)
                            if (!app.jobStartTime.isNullOrBlank()) {
                                append(" • ${app.jobStartTime}")
                                if (!app.jobEndTime.isNullOrBlank()) append("-${app.jobEndTime}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = GigColors.TextSecondary
                    )
                }
            }

            // ─── Employer Note ───────────────────────────────────────────────
            if (!app.employerNote.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GigColors.Info.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = GigColors.Info,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = app.employerNote!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = GigColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Progress Timeline (Phase 2: 7 steps) ───────────────────────
            ApplicationTimeline(
                status = app.status,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // ─── Action Button (Phase 2: context-aware for student/employer) ───
            when (app.status.lowercase()) {
                "pending" -> {
                    if (isEmployer) {
                        // Employer: Accept or Reject
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "Accept 🤝",
                                icon = Icons.Filled.CheckCircle,
                                color = GigColors.Success,
                                isLoading = isActionLoading,
                                onClick = onAccept,
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                text = "Reject",
                                icon = Icons.Filled.Cancel,
                                color = GigColors.Error,
                                isLoading = false,
                                onClick = onReject,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Student: Waiting
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = GigColors.Warning.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PulsingDot(color = GigColors.Warning)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Waiting for employer to review...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.Warning,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                "accepted" -> {
                    if (!isEmployer) {
                        // Student: Check In (on job day)
                        ActionButton(
                            text = "I'm On My Way 📍",
                            icon = Icons.Filled.NearMe,
                            color = GigColors.Info,
                            isLoading = isActionLoading,
                            onClick = onCheckIn
                        )
                    } else {
                        // Employer: Waiting for student to check in
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = GigColors.Info.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PulsingDot(color = GigColors.Info)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Waiting for student to check in...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.Info,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                "checked_in" -> {
                    if (isEmployer) {
                        // Employer: Confirm student arrived
                        ActionButton(
                            text = "Confirm Arrival ✔️",
                            icon = Icons.Filled.HowToReg,
                            color = GigColors.Primary,
                            isLoading = isActionLoading,
                            onClick = onConfirmArrival
                        )
                    } else {
                        // Student: Waiting for employer
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = GigColors.Info.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PulsingDot(color = GigColors.Info)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Waiting for employer to confirm your arrival...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.Info,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                "in_progress" -> {
                    if (!isEmployer) {
                        // Student: Mark work as done
                        ActionButton(
                            text = "Mark Work Done ✅",
                            icon = Icons.Filled.CheckCircle,
                            color = GigColors.Accent,
                            isLoading = isActionLoading,
                            onClick = onCompleteWork
                        )
                    } else {
                        // Employer: Waiting for student to finish
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = GigColors.Accent.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PulsingDot(color = GigColors.Accent)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Student is working...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.Accent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                "work_done" -> {
                    if (isEmployer) {
                        // Employer: Confirm completion quality
                        ActionButton(
                            text = "Confirm Completion ✨",
                            icon = Icons.Filled.Verified,
                            color = GigColors.Success,
                            isLoading = isActionLoading,
                            onClick = onConfirmCompletion
                        )
                    } else {
                        // Student: Waiting for employer confirmation
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = GigColors.Accent.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PulsingDot(color = GigColors.Accent)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Waiting for employer to confirm completion...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.Accent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                "confirmed" -> {
                    if (!isEmployer) {
                        // Student: Collect payment
                        ActionButton(
                            text = "Collect Payment 💰",
                            icon = Icons.Filled.CurrencyRupee,
                            color = GigColors.Warning,
                            isLoading = isActionLoading,
                            onClick = onConfirmPayment
                        )
                    } else {
                        // Employer: Waiting for student to collect
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = GigColors.Warning.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                PulsingDot(color = GigColors.Warning)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Waiting for student to collect payment...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.Warning,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                "completed" -> {
                    if (!isEmployer) {
                        // Legacy: old completed status
                        ActionButton(
                            text = "Confirm Payment Received",
                            icon = Icons.Filled.CurrencyRupee,
                            color = GigColors.Warning,
                            isLoading = isActionLoading,
                            onClick = onConfirmPayment
                        )
                    }
                }
                "paid" -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = GigColors.Success.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = GigColors.SuccessLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Job Complete — Payment Received! 🎉",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GigColors.SuccessLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Rating logic
                            if (app.rating == null) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onRate,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GigColors.Primary)
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Rate Experience", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = GigColors.Warning, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("You rated this experience", style = MaterialTheme.typography.labelSmall, color = GigColors.TextMuted)
                                }
                            }
                        }
                    }
                }
                "rejected" -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = GigColors.Error.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = GigColors.Error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Application not selected. Keep trying!",
                                style = MaterialTheme.typography.bodySmall,
                                color = GigColors.Error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  APPLICATION TIMELINE — Visual progress indicator
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun ApplicationTimeline(
    status: String,
    modifier: Modifier = Modifier
) {
    // Phase 2: 7-step timeline
    val steps = listOf(
        "Applied" to Icons.Filled.Send,
        "Accepted" to Icons.Filled.ThumbUp,
        "Checked In" to Icons.Filled.NearMe,
        "Working" to Icons.Filled.Build,
        "Done" to Icons.Filled.CheckCircle,
        "Confirmed" to Icons.Filled.Verified,
        "Paid" to Icons.Filled.CurrencyRupee
    )

    val currentStep = when (status.lowercase()) {
        "pending" -> 0
        "accepted" -> 1
        "checked_in" -> 2
        "in_progress" -> 3
        "work_done" -> 4
        "confirmed" -> 5
        "completed" -> 5  // Legacy compat
        "paid" -> 6
        "rejected" -> -1
        else -> 0
    }

    if (currentStep == -1) return // Don't show timeline for rejected

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (label, icon) ->
            val isActive = index <= currentStep
            val isCurrent = index == currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .staggeredSlideIn(index, baseDelayMs = 80)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 28.dp else 20.dp)
                        .clip(CircleShape)
                        .then(
                            if (isCurrent) Modifier.breathingGlow(
                                color = GigColors.Primary,
                                maxAlpha = 0.3f,
                                durationMs = 1500
                            ) else Modifier
                        )
                        .background(
                            if (isActive) {
                                if (isCurrent) GigColors.Primary
                                else GigColors.Success.copy(alpha = 0.8f)
                            } else GigColors.SurfaceHighest
                        )
                        .then(
                            if (isCurrent) Modifier.border(
                                2.dp,
                                GigColors.Primary.copy(alpha = 0.5f),
                                CircleShape
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (isActive) Color.White
                        else GigColors.TextMuted,
                        modifier = Modifier.size(if (isCurrent) 14.dp else 11.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) GigColors.TextPrimary
                    else GigColors.TextMuted,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }

            // Connector line
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.4f)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (index < currentStep) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        GigColors.Success.copy(alpha = 0.8f),
                                        if (index < currentStep - 1) GigColors.Success.copy(alpha = 0.8f)
                                        else GigColors.Primary.copy(alpha = 0.6f)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        GigColors.SurfaceHighest,
                                        GigColors.SurfaceHighest
                                    )
                                )
                            }
                        )
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  ACTION BUTTON — Gradient call-to-action
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.4f)
        ),
        enabled = !isLoading,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
            Text("Processing...", color = Color.White)
        } else {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  PULSING DOT — Animated waiting indicator
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .size((10 * scale).dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  STAT CHIP
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun AppStatChip(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
            )
        }
    }
}
