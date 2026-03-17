package com.studentgig.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.data.model.NotificationItem
import com.studentgig.app.ui.animations.cascadeEntrance
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.NotificationsViewModel


// ═══════════════════════════════════════════════════════════════════════════════════
//  PHASE 6: NOTIFICATIONS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = GigColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = GigColors.TextPrimary
                        )
                        if (state.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = GigColors.Error
                            ) {
                                Text(
                                    text = "${state.unreadCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GigColors.TextPrimary
                        )
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllRead() }) {
                            Text(
                                "Mark All Read",
                                color = GigColors.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GigColors.Background
                )
            )
        }
    ) { padding ->
        val pullRefreshState = rememberPullToRefreshState()
        val isRefreshing = state.isLoading && state.notifications.isNotEmpty()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullToRefresh(
                    isRefreshing = isRefreshing,
                    state = pullRefreshState,
                    onRefresh = { viewModel.refresh() }
                )
        ) {
        when {
            !state.isLoggedIn -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = GigColors.TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Log in to see your notifications",
                            color = GigColors.TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GigColors.Primary,
                        strokeWidth = 3.dp
                    )
                }
            }
            state.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Animated bell icon
                        val infiniteTransition = rememberInfiniteTransition(label = "bell")
                        val rotate by infiniteTransition.animateFloat(
                            initialValue = -10f,
                            targetValue = 10f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bellRotate"
                        )
                        Icon(
                            Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = GigColors.TextMuted,
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer { rotationZ = rotate }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No notifications yet",
                            color = GigColors.TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "You'll see updates about your applications and jobs here",
                            color = GigColors.TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        val index = state.notifications.indexOf(notification)
                        Box(
                            modifier = Modifier.cascadeEntrance(
                                index = index,
                                baseDelayMs = 30,
                                maxDelayMs = 200
                            )
                        ) {
                            NotificationCard(notification = notification)
                        }
                    }
                    // Bottom spacer
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

            // Pull-to-refresh indicator
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = GigColors.Primary
            )
        } // Box
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  NOTIFICATION CARD
// ═══════════════════════════════════════════════════════════════════════════════════

@Composable
fun NotificationCard(
    notification: NotificationItem,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, bgColor) = getNotificationStyle(notification.type)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead)
                GigColors.Primary.copy(alpha = 0.05f)
            else GigColors.SurfaceElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = GigColors.TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GigColors.Primary)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    color = GigColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Relative time
                notification.createdAt?.let { time ->
                    Text(
                        text = formatRelativeTime(time),
                        color = GigColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════════
//  HELPERS
// ═══════════════════════════════════════════════════════════════════════════════════

data class NotificationStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color
)

fun getNotificationStyle(type: String): NotificationStyle {
    return when {
        type.contains("accepted") -> NotificationStyle(
            Icons.Filled.CheckCircle, GigColors.Success,
            GigColors.Success.copy(alpha = 0.12f)
        )
        type.contains("rejected") -> NotificationStyle(
            Icons.Filled.Cancel, GigColors.Error,
            GigColors.Error.copy(alpha = 0.1f)
        )
        type.contains("check_in") -> NotificationStyle(
            Icons.Filled.DirectionsWalk, GigColors.Info,
            GigColors.Info.copy(alpha = 0.12f)
        )
        type.contains("work_started") -> NotificationStyle(
            Icons.Filled.PlayArrow, GigColors.Primary,
            GigColors.Primary.copy(alpha = 0.12f)
        )
        type.contains("work_done") -> NotificationStyle(
            Icons.Filled.Flag, GigColors.Warning,
            GigColors.Warning.copy(alpha = 0.12f)
        )
        type.contains("payment") -> NotificationStyle(
            Icons.Filled.AccountBalanceWallet, GigColors.Success,
            GigColors.Success.copy(alpha = 0.12f)
        )
        type.contains("rating") -> NotificationStyle(
            Icons.Filled.Star, GigColors.Warning,
            GigColors.Warning.copy(alpha = 0.12f)
        )
        type.contains("ai_job_match") -> NotificationStyle(
            Icons.Filled.AutoAwesome, GigColors.Accent,
            GigColors.Accent.copy(alpha = 0.12f)
        )
        else -> NotificationStyle(
            Icons.Filled.Notifications, GigColors.Primary,
            GigColors.Primary.copy(alpha = 0.12f)
        )
    }
}

fun formatRelativeTime(isoTime: String): String {
    return try {
        val instant = java.time.Instant.parse(isoTime)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)

        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            duration.toDays() < 7 -> "${duration.toDays()}d ago"
            else -> "${duration.toDays() / 7}w ago"
        }
    } catch (_: Exception) {
        ""
    }
}
