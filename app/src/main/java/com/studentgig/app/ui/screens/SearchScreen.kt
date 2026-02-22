package com.studentgig.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Search Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GigGradients.HeaderGlow)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Search Gigs",
                        fontSize = 26.sp,
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

                    // Search bar
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        placeholder = { Text("Search by title, skill…") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = GigColors.Primary
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GigColors.Primary,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
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
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = GigColors.BorderSubtle,
                                selectedBorderColor = GigColors.Error.copy(alpha = 0.5f),
                                enabled = true,
                                selected = uiState.urgentOnly
                            )
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
                                shape = RoundedCornerShape(12.dp),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = GigColors.BorderSubtle,
                                    selectedBorderColor = GigColors.Primary.copy(alpha = 0.5f),
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    // ─── Pay Range Filter ────────────────────────────────────
                    var showPayFilter by remember { mutableStateOf(false) }
                    var payRange by remember { mutableStateOf(100f..10000f) }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilterChip(
                        selected = showPayFilter || (uiState.minPay != null || uiState.maxPay != null),
                        onClick = {
                            showPayFilter = !showPayFilter
                            if (!showPayFilter) {
                                viewModel.onPayRangeChanged(null, null)
                                viewModel.search()
                            }
                        },
                        label = {
                            Text(
                                text = if (uiState.minPay != null || uiState.maxPay != null)
                                    "₹${payRange.start.toInt()} – ₹${payRange.endInclusive.toInt()}"
                                else "💰 Pay Range",
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GigColors.SurfaceElevated,
                            selectedContainerColor = GigColors.SuccessMuted,
                            labelColor = GigColors.TextSecondary,
                            selectedLabelColor = GigColors.Success,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = GigColors.BorderSubtle,
                            selectedBorderColor = GigColors.Success.copy(alpha = 0.5f),
                            enabled = true,
                            selected = showPayFilter
                        )
                    )

                    if (showPayFilter) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("₹${payRange.start.toInt()}", color = GigColors.TextSecondary, fontSize = 12.sp)
                            Text("₹${payRange.endInclusive.toInt()}", color = GigColors.TextSecondary, fontSize = 12.sp)
                        }
                        RangeSlider(
                            value = payRange,
                            onValueChange = { payRange = it },
                            onValueChangeFinished = {
                                viewModel.onPayRangeChanged(
                                    payRange.start.toDouble(),
                                    payRange.endInclusive.toDouble()
                                )
                                viewModel.search()
                            },
                            valueRange = 100f..10000f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = GigColors.Primary,
                                activeTrackColor = GigColors.Primary,
                                inactiveTrackColor = GigColors.Border,
                            )
                        )
                    }
                }
            }

            // ─── Results ────────────────────────────────────────────────────
            when {
                uiState.isSearching -> {
                    GigShimmerLoading(cardCount = 3)
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        GigRetryBanner(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.search() }
                        )
                    }
                }
                !uiState.hasSearched -> {
                    GigEmptyState(
                        icon = Icons.Outlined.TravelExplore,
                        title = "Start exploring",
                        subtitle = "Type a keyword or tap a filter to discover gigs"
                    )
                }
                uiState.results.isEmpty() -> {
                    GigEmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No gigs found",
                        subtitle = "Try different keywords or filters"
                    )
                }
                else -> {
                    val listState = rememberLazyListState()

                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val totalItemsCount = listState.layoutInfo.totalItemsCount
                            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleItemIndex >= (totalItemsCount - 3)
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && !uiState.isSearching && !uiState.isLoadingMore && !uiState.isLastPage) {
                            viewModel.loadMoreSearch()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "${uiState.results.size} result${if (uiState.results.size != 1) "s" else ""}",
                                fontSize = 13.sp,
                                color = GigColors.TextMuted,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(uiState.results, key = { it.id }) { job ->
                            GigJobCard(
                                job = job,
                                isApplying = uiState.isApplying,
                                onApplyClick = { viewModel.onApplyClicked(job.id) },
                                onCardClick = { onJobClick(job.id) },
                                isApplied = job.id in uiState.appliedJobIds,
                            )
                        }

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
                        } else if (uiState.isLastPage && uiState.results.isNotEmpty()) {
                            item {
                                Text(
                                    text = "End of results.",
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
            }
        }
    }
}
