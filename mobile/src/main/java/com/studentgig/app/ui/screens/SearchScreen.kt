package com.studentgig.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.ui.animations.cascadeEntrance
import com.studentgig.app.ui.animations.glassSurface
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onJobClick: (Int) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

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
            onGoogleLogin = { idToken -> viewModel.onGoogleLogin(idToken) },
            onFirebaseLogin = { idToken, name -> viewModel.onFirebaseLogin(idToken, name) },
            onDismiss = { viewModel.dismissLoginSheet() }
        )
    }

    // Animated search bar border glow when focused
    var isSearchFocused by remember { mutableStateOf(false) }
    val borderGlowAlpha by animateFloatAsState(
        targetValue = if (isSearchFocused) 1f else 0f,
        animationSpec = tween(300),
        label = "searchGlow"
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // ─── Search Header ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GigGradients.HeaderGlow)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Search Jobs",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GigColors.TextPrimary,
                    letterSpacing = (-0.75).sp
                )
                Text(
                    text = "Find the perfect opportunity",
                    fontSize = 13.sp,
                    color = GigColors.TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // ─── Search Mode Toggle ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GigColors.SurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val aiSelected = uiState.useAiSearch
                    val basicSelected = !uiState.useAiSearch
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (basicSelected) GigColors.SurfaceHighest else Color.Transparent)
                            .clickable { viewModel.onAiToggle(false) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Basic Keyword", fontSize = 14.sp, fontWeight = if (basicSelected) FontWeight.Bold else FontWeight.Medium, color = if (basicSelected) GigColors.TextPrimary else GigColors.TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (aiSelected) GigColors.SurfaceHighest else Color.Transparent)
                            .clickable { viewModel.onAiToggle(true) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨ Smart Search", fontSize = 14.sp, fontWeight = if (aiSelected) FontWeight.Bold else FontWeight.Medium, color = if (aiSelected) GigColors.Primary else GigColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (borderGlowAlpha > 0f) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            GigColors.Primary.copy(alpha = 0.6f * borderGlowAlpha),
                                            GigColors.Accent.copy(alpha = 0.4f * borderGlowAlpha),
                                            GigColors.Primary.copy(alpha = 0.6f * borderGlowAlpha)
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            } else Modifier
                        )
                ) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        placeholder = {
                            Text(
                                if (uiState.useAiSearch) "Describe the job you want naturally..."
                                else "Search by title, skill, keyword…",
                                color = GigColors.TextMuted.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = if (isSearchFocused) GigColors.Primary else GigColors.TextMuted
                            )
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                    Icon(Icons.Filled.Clear, "Clear", tint = GigColors.TextMuted)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.search()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = GigColors.Border,
                            cursorColor = GigColors.Primary,
                            focusedTextColor = GigColors.TextPrimary,
                            unfocusedTextColor = GigColors.TextPrimary,
                            focusedContainerColor = GigColors.SurfaceElevated,
                            unfocusedContainerColor = GigColors.SurfaceElevated,
                            focusedPlaceholderColor = GigColors.TextMuted,
                            unfocusedPlaceholderColor = GigColors.TextMuted,
                        )
                    )
                }
            }

            // ─── Filters & Results ──────────────────────────────────────────
            val listState = rememberLazyListState()
            val pullState = rememberPullToRefreshState()

            PullToRefreshBox(
                state = pullState,
                isRefreshing = uiState.isSearching,
                onRefresh = { viewModel.search() },
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = uiState.isSearching,
                        state = pullState,
                        containerColor = GigColors.SurfaceElevated,
                        color = GigColors.Primary,
                    )
                }
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Filters as list items to ensure they scroll
                    if (!uiState.useAiSearch) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = uiState.urgentOnly,
                                        onClick = { viewModel.onUrgentToggle(!uiState.urgentOnly) },
                                        label = { Text("🔥 Urgent", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = GigColors.SurfaceElevated,
                                            selectedContainerColor = GigColors.ErrorMuted,
                                            labelColor = GigColors.TextSecondary,
                                            selectedLabelColor = GigColors.Error,
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    listOf("Hyderabad", "Bangalore", "Remote").forEach { loc ->
                                        val isSelected = uiState.location == loc
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.onLocationChanged(if (isSelected) "" else loc)
                                                viewModel.search()
                                            },
                                            label = { Text(loc, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = GigColors.SurfaceElevated,
                                                selectedContainerColor = GigColors.PrimaryMuted,
                                                labelColor = GigColors.TextSecondary,
                                                selectedLabelColor = GigColors.PrimaryLight,
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                                
                                // Date Filters
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("" to "📅 Any", "today" to "📌 Today", "tomorrow" to "📆 Tomorrow").forEach { (value, label) ->
                                        val isSelected = uiState.dateFilter == value
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.onDateFilterChanged(value) },
                                            label = { Text(label, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = GigColors.SurfaceElevated,
                                                selectedContainerColor = GigColors.Accent.copy(alpha = 0.15f),
                                                labelColor = GigColors.TextSecondary,
                                                selectedLabelColor = GigColors.Accent,
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.aiInterpretation != null) {
                        item {
                            Surface(
                                color = GigColors.Primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, GigColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    "✨ ${uiState.aiInterpretation}",
                                    color = GigColors.PrimaryDark,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Results
                    when {
                        uiState.isSearching && uiState.results.isEmpty() -> {
                            item { GigShimmerLoading(cardCount = 3) }
                        }
                        !uiState.hasSearched -> {
                            item {
                                Box(Modifier.fillParentMaxHeight(0.6f), contentAlignment = Alignment.Center) {
                                    GigEmptyState(icon = Icons.Outlined.TravelExplore, title = "Start exploring", subtitle = "Find jobs by keyword or location")
                                }
                            }
                        }
                        uiState.results.isEmpty() -> {
                            item {
                                Box(Modifier.fillParentMaxHeight(0.6f), contentAlignment = Alignment.Center) {
                                    GigEmptyState(icon = Icons.Outlined.SearchOff, title = "No jobs found", subtitle = "Try adjusting your filters")
                                }
                            }
                        }
                        else -> {
                            item {
                                Text(
                                    "${uiState.results.size} results found",
                                    fontSize = 13.sp,
                                    color = GigColors.TextMuted,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            items(uiState.results, key = { it.id }) { job ->
                                Box(Modifier.cascadeEntrance(index = uiState.results.indexOf(job))) {
                                    GigJobCard(
                                        job = job,
                                        isApplying = uiState.isApplying,
                                        onApplyClick = { viewModel.onApplyClicked(job.id) },
                                        onCardClick = { onJobClick(job.id) },
                                        isApplied = job.id in uiState.appliedJobIds,
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
