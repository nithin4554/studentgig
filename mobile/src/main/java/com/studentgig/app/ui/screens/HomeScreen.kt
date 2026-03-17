package com.studentgig.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.ui.animations.AmbientGlowOrbs
import com.studentgig.app.ui.animations.cascadeEntrance
import com.studentgig.app.ui.animations.floatingEffect
import com.studentgig.app.ui.animations.breathingGlow
import com.studentgig.app.ui.animations.glassSurface
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGlow
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.HomeViewModel

// ─── Home Screen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onJobClick: (Int) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Re-check login state when this screen becomes visible
    // (handles cases where user logged in from Profile/Activity tab)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAuthState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            onGoogleLogin = { idToken -> viewModel.onGoogleLogin(idToken) },
            onFirebaseLogin = { idToken, name -> viewModel.onFirebaseLogin(idToken, name) },
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
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())
        ) {
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
                        title = "No jobs available",
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
                                    jobCount = uiState.jobs.size,
                                    unreadNotificationCount = unreadNotificationCount,
                                    onNotificationsClick = onNotificationsClick,
                                    modifier = Modifier.graphicsLayer {
                                        if (listState.firstVisibleItemIndex == 0) {
                                            translationY = listState.firstVisibleItemScrollOffset * 0.5f
                                            alpha = 1f - (listState.firstVisibleItemScrollOffset / 600f).coerceIn(0f, 1f)
                                        }
                                    }
                                )
                            }

                            // ─── Smart Discovery Banner ──────────────────────────
                            item {
                                SmartDiscoveryBanner(
                                    insights = uiState.aiInsights?.tips?.ifEmpty { uiState.aiInsights?.insights },
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                        .cascadeEntrance(index = 1, baseDelayMs = 150)
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
                                        Row(
                                            modifier = Modifier
                                                .background(GigColors.SurfaceHighest, RoundedCornerShape(12.dp))
                                                .padding(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val latestSelected = uiState.feedMode == "latest"
                                            val forYouSelected = uiState.feedMode == "foryou"
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (latestSelected) GigColors.SurfaceElevated else Color.Transparent)
                                                    .clickable { viewModel.setFeedMode("latest") }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "Latest", 
                                                    fontSize = 14.sp, 
                                                    fontWeight = if (latestSelected) FontWeight.Bold else FontWeight.Medium, 
                                                    color = if (latestSelected) GigColors.TextPrimary else GigColors.TextSecondary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (forYouSelected) GigColors.SurfaceElevated else Color.Transparent)
                                                    .clickable { 
                                                        if (uiState.isLoggedIn) viewModel.setFeedMode("foryou")
                                                        else viewModel.onApplyClicked(-1) // triggers login sheet
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "✨ For You", 
                                                    fontSize = 14.sp, 
                                                    fontWeight = if (forYouSelected) FontWeight.Bold else FontWeight.Medium, 
                                                    color = if (forYouSelected) GigColors.Primary else GigColors.TextSecondary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${uiState.jobs.size} opportunities",
                                            color = GigColors.TextMuted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    if (uiState.feedMode == "latest") {
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
                            }

                            // ─── Job Cards with Cascade Spring Entrance ───────────
                            items(
                                sortedJobs,
                                key = { it.id }
                            ) { job ->
                                val index = sortedJobs.indexOf(job)

                                Box(
                                    modifier = Modifier
                                        .animateItem()
                                        .cascadeEntrance(
                                            index = index,
                                            baseDelayMs = 50,
                                            maxDelayMs = 350
                                        )
                                        .padding(horizontal = 16.dp)
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

                            // ─── Loading More Indicator ───────────────────────────
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
                                    // Premium end-of-list indicator
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            GigColors.Primary.copy(alpha = 0.4f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "You've caught up! No more jobs.",
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp,
                                            color = GigColors.TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }

                    // ─── Scroll-to-Top FAB ────────────────────────────────────
                    AnimatedVisibility(
                        visible = showFab,
                        enter = fadeIn() + scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
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
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(52.dp)
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
    unreadNotificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GigGradients.HeroAmbient)
    ) {
        // Ambient Glow Orbs — ethereal living background (Comet-inspired)
        AmbientGlowOrbs(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            orbCount = 3,
            primaryColor = GigGlow.OrbPrimary,
            accentColor = GigGlow.OrbAccent,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Top row: Greeting + Server status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) "Welcome back," else "Hello there! 👋",
                        color = GigColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLoggedIn) userName ?: "Student" else "Discover Jobs",
                        color = GigColors.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.75).sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Phase 6: Notification Bell with Badge
                    if (isLoggedIn) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = GigColors.Error,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = onNotificationsClick) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = GigColors.TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    GigServerDot(isOnline = isOnline, isChecking = isChecking)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row with subtle floating effect
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .floatingEffect(amplitude = 2.dp, durationMs = 4000),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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


// ─── Smart Discovery Banner ───────────────────────────────────────────────────────

@Composable
fun SmartDiscoveryBanner(insights: List<String>?, modifier: Modifier = Modifier) {
    Surface(
        color = GigColors.Primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                GigColors.Primary.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GigColors.Primary)
                    .breathingGlow(color = GigColors.Primary.copy(alpha=0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (insights.isNullOrEmpty()) Icons.Filled.PostAdd else Icons.Filled.Lightbulb, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (insights.isNullOrEmpty()) {
                    Text("Need help with a task?", fontWeight = FontWeight.Bold, color = GigColors.TextPrimary, fontSize = 16.sp)
                    Text("Post a gig in 60 seconds and find students ready to help.", color = GigColors.TextSecondary, fontSize = 13.sp)
                } else {
                    val currentInsight = remember(insights) { insights.random() }
                    Text("✨ AI Insight", fontWeight = FontWeight.Bold, color = GigColors.Primary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(currentInsight, color = GigColors.TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
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
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
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
