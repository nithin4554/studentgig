package com.studentgig.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.HomeViewModel

// ─── Home Screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onJobClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar for apply messages
    LaunchedEffect(uiState.applyMessage) {
        uiState.applyMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissApplyMessage()
        }
    }

    // Login sheet
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
        }
    ) { padding ->
        when {
            // Server offline
            !uiState.isServerOnline && !uiState.isCheckingServer -> {
                GigOfflineState(
                    errorMessage = uiState.errorMessage ?: "Start the Python backend on port 8000",
                    onRetry = { viewModel.retry() }
                )
            }
            // Loading — show shimmer skeleton cards
            uiState.isCheckingServer || uiState.isLoadingJobs -> {
                GigShimmerLoading(cardCount = 4)
            }
            // Empty
            uiState.jobs.isEmpty() -> {
                GigEmptyState(
                    icon = Icons.Outlined.WorkOutline,
                    title = "No gigs available",
                    subtitle = "New opportunities are posted every day!"
                )
            }
            // Content
            else -> {
                val pullState = rememberPullToRefreshState()
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                val showFab by remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 1 }
                }

                // Sort state
                var sortMode by remember { mutableStateOf("default") }
                var showSortMenu by remember { mutableStateOf(false) }

                val sortedJobs = remember(uiState.jobs, sortMode) {
                    when (sortMode) {
                        "pay_high" -> uiState.jobs.sortedByDescending { it.payAmount }
                        "pay_low" -> uiState.jobs.sortedBy { it.payAmount }
                        "match" -> uiState.jobs.sortedByDescending { it.matchScore ?: 0 }
                        "urgent" -> uiState.jobs.sortedByDescending { it.isUrgent }
                        "latest" -> uiState.jobs.sortedByDescending { it.createdAt }
                        else -> uiState.jobs
                    }
                }

                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                ) {
                    PullToRefreshBox(
                        state = pullState,
                        isRefreshing = uiState.isLoadingJobs,
                        onRefresh = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            Indicator(
                                modifier = Modifier.align(Alignment.TopCenter),
                                isRefreshing = uiState.isLoadingJobs,
                                state = pullState,
                                containerColor = GigColors.SurfaceElevated,
                                color = GigColors.Primary,
                            )
                        }
                    ) {
                        // Infinite Scroll implementation
                        val shouldLoadMore = remember {
                            derivedStateOf {
                                val totalItemsCount = listState.layoutInfo.totalItemsCount
                                val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                // Trigger load when 3 items away from the end
                                lastVisibleItemIndex >= (totalItemsCount - 3)
                            }
                        }

                        LaunchedEffect(shouldLoadMore.value) {
                            if (shouldLoadMore.value && !uiState.isLoadingJobs && !uiState.isLoadingMore && !uiState.isLastPage) {
                                viewModel.loadMoreJobs()
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            // ─── Hero Header ────────────────────────────────────
                            item {
                                HeroBanner(
                                    isLoggedIn = uiState.isLoggedIn,
                                    userName = uiState.userName,
                                    isOnline = uiState.isServerOnline,
                                    isChecking = uiState.isCheckingServer,
                                    jobCount = uiState.jobs.size
                                )
                            }

                            // ─── Section Header + Sort ───────────────────────────
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Available Gigs",
                                            color = GigColors.TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp
                                        )
                                        Text(
                                            text = "${uiState.jobs.size} opportunities",
                                            color = GigColors.TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Box {
                                        FilledTonalButton(
                                            onClick = { showSortMenu = true },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = GigColors.SurfaceHighest,
                                                contentColor = GigColors.TextSecondary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Sort,
                                                contentDescription = "Sort",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when(sortMode) {
                                                    "pay_high" -> "Pay ↓"
                                                    "pay_low" -> "Pay ↑"
                                                    "match" -> "Match"
                                                    "urgent" -> "Urgent"
                                                    "latest" -> "Latest"
                                                    else -> "Sort"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false },
                                            containerColor = GigColors.SurfaceElevated,
                                        ) {
                                            listOf(
                                                "default" to "Default",
                                                "match" to "AI Match Score",
                                                "pay_high" to "Highest Pay",
                                                "pay_low" to "Lowest Pay",
                                                "urgent" to "Urgent First",
                                                "latest" to "Latest"
                                            ).forEach { (key, label) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            label,
                                                            color = if (sortMode == key) GigColors.Primary
                                                                    else GigColors.TextPrimary,
                                                            fontWeight = if (sortMode == key) FontWeight.Bold
                                                                        else FontWeight.Normal,
                                                            fontSize = 14.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        sortMode = key
                                                        showSortMenu = false
                                                    },
                                                    leadingIcon = {
                                                        if (sortMode == key) {
                                                            Icon(
                                                                Icons.Filled.Check,
                                                                contentDescription = null,
                                                                tint = GigColors.Primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ─── Animated Job Cards ──────────────────────────────
                            items(
                                sortedJobs,
                                key = { it.id }
                            ) { job ->
                                val index = sortedJobs.indexOf(job)
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { visible = true }

                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(
                                        animationSpec = tween(
                                            durationMillis = 350,
                                            delayMillis = (index * 60).coerceAtMost(300)
                                        )
                                    ) + slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(
                                            durationMillis = 350,
                                            delayMillis = (index * 60).coerceAtMost(300)
                                        )
                                    )
                                ) {
                                    GigJobCard(
                                        job = job,
                                        isApplying = uiState.isApplying,
                                        onApplyClick = { viewModel.onApplyClicked(job.id) },
                                        onCardClick = { onJobClick(job.id) },
                                        isApplied = job.id in uiState.appliedJobIds,
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            item { Spacer(modifier = Modifier.height(12.dp)) }

                            // ─── Loading More Indicator ───────────────────────────────────
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = GigColors.Primary,
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            } else if (uiState.isLastPage && sortedJobs.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "You've caught up! No more gigs.",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        color = GigColors.TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }

                    // ─── Scroll-to-Top FAB ────────────────────────────────────
                    AnimatedVisibility(
                        visible = showFab,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            },
                            containerColor = GigColors.Primary,
                            contentColor = GigColors.TextOnPrimary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Scroll to top")
                        }
                    }
                }
            }
        }
    }
}


// ─── Hero Banner ────────────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(
    isLoggedIn: Boolean,
    userName: String?,
    isOnline: Boolean,
    isChecking: Boolean,
    jobCount: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GigGradients.HeaderGlow)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            // Top row: Greeting + Server status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isLoggedIn) "Welcome back," else "Hello there! 👋",
                        color = GigColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isLoggedIn) userName ?: "Student" else "Discover Gigs",
                        color = GigColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.75).sp
                    )
                }
                GigServerDot(isOnline = isOnline, isChecking = isChecking)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    icon = Icons.Filled.Work,
                    label = "$jobCount Active",
                    color = GigColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    icon = Icons.Filled.TrendingUp,
                    label = "Trending",
                    color = GigColors.Success,
                    modifier = Modifier.weight(1f)
                )
                if (isLoggedIn) {
                    StatChip(
                        icon = Icons.Filled.AutoAwesome,
                        label = "AI Matched",
                        color = GigColors.Accent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


// ─── Stat Chip ──────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
