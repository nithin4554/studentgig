package com.studentgig.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studentgig.app.ui.screens.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGlow
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.ApplicationsViewModel
import com.studentgig.app.ui.viewmodel.NotificationsViewModel

// ─── Routes ─────────────────────────────────────────────────────────────────────

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SEARCH = "search"
    const val APPLICATIONS = "applications"
    const val EMPLOYER_DASHBOARD = "employer_dashboard"
    const val PROFILE = "profile"
    const val JOB_DETAIL = "job_detail/{jobId}"
    const val POST_JOB = "post_job"
    const val NOTIFICATIONS = "notifications"

    fun jobDetail(jobId: Int) = "job_detail/$jobId"
}

// ─── Bottom Nav Items ───────────────────────────────────────────────────────────

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Routes.APPLICATIONS, "Activity", Icons.Filled.WorkHistory, Icons.Outlined.WorkHistory),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
)

// ─── Main Navigation Host ───────────────────────────────────────────────────────

@Composable
fun StudentGigNavHost(deepLinkJobId: Int? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Handle deep link startup
    LaunchedEffect(deepLinkJobId) {
        if (deepLinkJobId != null) {
            navController.navigate(Routes.jobDetail(deepLinkJobId)) {
                popUpTo(Routes.HOME) { saveState = true }
            }
        }
    }

    // Application badge count
    val appsViewModel: ApplicationsViewModel = hiltViewModel()
    val appsState by appsViewModel.uiState.collectAsState()
    val pendingCount = appsState.applications.count { it.status.lowercase() == "pending" }

    // Phase 6: Notification badge count
    val notifViewModel: NotificationsViewModel = hiltViewModel()
    val notifState by notifViewModel.uiState.collectAsState()
    LaunchedEffect(currentRoute) { notifViewModel.fetchUnreadCount() }

    // Hide bottom bar on detail/splash screens
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    val context = androidx.compose.ui.platform.LocalContext.current
    var showFabTooltip by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("GigPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_seen_fab", false)) {
            showFabTooltip = true
            kotlinx.coroutines.delay(6000)
            showFabTooltip = false
            prefs.edit().putBoolean("has_seen_fab", true).apply()
        }
    }

    var backPressedOnce by remember { mutableStateOf(false) }
    val isOnMainTab = currentRoute in bottomNavItems.map { it.route }

    BackHandler(enabled = isOnMainTab) {
        if (backPressedOnce) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedOnce = true
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000)
            backPressedOnce = false
        }
    }

    Scaffold(
        containerColor = GigColors.Background,
        floatingActionButton = {
            if (showBottomBar) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (showFabTooltip) {
                                showFabTooltip = false
                                context.getSharedPreferences("GigPrefs", android.content.Context.MODE_PRIVATE)
                                    .edit().putBoolean("has_seen_fab", true).apply()
                            }
                            navController.navigate(Routes.POST_JOB)
                        },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = GigColors.Primary,
                        contentColor = Color.White,
                        modifier = Modifier.shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = GigColors.Primary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Post Job", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                PremiumBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    pendingCount = pendingCount,
                    onItemClick = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onSplashComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onJobClick = { jobId -> navController.navigate(Routes.jobDetail(jobId)) },
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    unreadNotificationCount = notifState.unreadCount
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onJobClick = { jobId -> navController.navigate(Routes.jobDetail(jobId)) })
            }
            composable(Routes.APPLICATIONS) {
                MyApplicationsScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onLogout = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                })
            }
            composable(
                route = Routes.JOB_DETAIL,
                arguments = listOf(navArgument("jobId") { type = NavType.IntType }),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
            ) { backStack ->
                val jobId = backStack.arguments?.getInt("jobId") ?: 0
                JobDetailScreen(jobId = jobId, onBack = { navController.popBackStack() })
            }
            composable(Routes.POST_JOB) {
                PostJobScreen(
                    onBack = { navController.popBackStack() },
                    onJobPosted = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = {
                    notifViewModel.fetchUnreadCount()
                    navController.popBackStack()
                })
            }
        }
    }
}

// ─── Nav Bar Components (Simplified for stability) ──────────────────────────────

@Composable
private fun PremiumBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    pendingCount: Int,
    onItemClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(GigColors.Surface.copy(alpha = 0.98f))
                .border(1.dp, GigGlow.GlassBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                PremiumNavItem(
                    item = item,
                    isSelected = isSelected,
                    pendingCount = if (item.route == Routes.APPLICATIONS) pendingCount else 0,
                    onClick = { onItemClick(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PremiumNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BadgedBox(
                badge = {
                    if (pendingCount > 0) {
                        Badge(containerColor = GigColors.Warning) {
                            Text(if (pendingCount > 9) "9+" else "$pendingCount", fontSize = 9.sp)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = if (isSelected) GigColors.Primary else GigColors.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = item.label,
                color = if (isSelected) GigColors.Primary else GigColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
