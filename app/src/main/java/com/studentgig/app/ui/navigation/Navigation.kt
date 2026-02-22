package com.studentgig.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.studentgig.app.ui.viewmodel.ApplicationsViewModel

// ─── Routes ─────────────────────────────────────────────────────────────────────

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val APPLICATIONS = "applications"
    const val PROFILE = "profile"
    const val JOB_DETAIL = "job_detail/{jobId}"

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
    BottomNavItem(Routes.APPLICATIONS, "My Jobs", Icons.Filled.WorkHistory, Icons.Outlined.WorkHistory),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle),
)

// ─── Main Navigation Host ───────────────────────────────────────────────────────

@Composable
fun StudentGigNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Application badge count
    val appsViewModel: ApplicationsViewModel = hiltViewModel()
    val appsState by appsViewModel.uiState.collectAsState()
    val pendingCount = appsState.applications.count { it.status.lowercase() == "pending" }

    // Hide bottom bar on detail screens
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = GigColors.Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = GigColors.Surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .shadow(
                            elevation = 20.dp,
                            ambientColor = Color.Black.copy(alpha = 0.6f),
                            spotColor = Color.Black.copy(alpha = 0.4f)
                        )
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (item.route == Routes.APPLICATIONS && pendingCount > 0) {
                                            Badge(
                                                containerColor = GigColors.Warning,
                                                contentColor = GigColors.Background
                                            ) {
                                                Text(
                                                    text = if (pendingCount > 9) "9+" else "$pendingCount",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GigColors.Primary,
                                selectedTextColor = GigColors.Primary,
                                unselectedIconColor = GigColors.TextMuted,
                                unselectedTextColor = GigColors.TextMuted,
                                indicatorColor = GigColors.PrimaryMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(250))
            }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onJobClick = { jobId ->
                        navController.navigate(Routes.jobDetail(jobId))
                    }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onJobClick = { jobId ->
                        navController.navigate(Routes.jobDetail(jobId))
                    }
                )
            }
            composable(Routes.APPLICATIONS) {
                MyApplicationsScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(
                route = Routes.JOB_DETAIL,
                arguments = listOf(navArgument("jobId") { type = NavType.IntType }),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(200))
                }
            ) { backStack ->
                val jobId = backStack.arguments?.getInt("jobId") ?: 0
                JobDetailScreen(
                    jobId = jobId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
