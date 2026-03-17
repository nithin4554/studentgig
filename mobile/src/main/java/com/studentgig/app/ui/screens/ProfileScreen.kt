package com.studentgig.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.ui.animations.AmbientGlowOrbs
import com.studentgig.app.ui.animations.PulseGlow
import com.studentgig.app.ui.animations.breathingGlow
import com.studentgig.app.ui.animations.cascadeEntrance
import com.studentgig.app.ui.animations.staggeredSlideIn
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGlow
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.viewmodel.ProfileViewModel
import com.studentgig.app.ui.viewmodel.ProfileUiState

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh profile when screen becomes visible (handles login from other tabs)
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

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val msg = uiState.successMessage ?: uiState.errorMessage
        msg?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissMessage()
        }
    }

    // Login sheet (triggered from Profile tab when not logged in)
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
                !uiState.isLoggedIn -> {
                    NotLoggedInState(onSignInClick = { viewModel.showLogin() })
                }
                uiState.isLoading -> {
                    GigLoadingState(message = "Loading profile…")
                }
                uiState.isEditing -> {
                    EditProfileContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier
                    )
                }
                else -> {
                    ViewProfileContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onLogout = {
                            viewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}


// ─── Not Logged In ──────────────────────────────────────────────────────────────

@Composable
private fun NotLoggedInState(onSignInClick: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .breathingGlow(color = GigColors.Primary, maxAlpha = 0.2f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GigColors.Primary.copy(alpha = 0.15f),
                                GigColors.PrimaryMuted
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PersonOff,
                    contentDescription = null,
                    tint = GigColors.Primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Not Signed In",
                color = GigColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Sign in to manage your profile,\ntrack applications & unlock AI matching",
                color = GigColors.TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Sign In button
            GigGradientButton(
                text = "Sign In",
                onClick = onSignInClick,
                icon = Icons.AutoMirrored.Filled.Login,
                modifier = Modifier.fillMaxWidth(0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtle info text
            Text(
                text = "Use your phone number or Google account",
                color = GigColors.TextMuted.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


// ─── View Profile ───────────────────────────────────────────────────────────────

@Composable
private fun ViewProfileContent(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = uiState.user ?: return
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ─── Profile Header with Avatar ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GigGradients.HeroAmbient),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glow behind avatar
            AmbientGlowOrbs(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                orbCount = 2,
                primaryColor = GigGlow.OrbPrimary,
                accentColor = GigGlow.OrbAccent,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 24.dp, bottom = 32.dp)
            ) {
                // Avatar with Pulse Glow + Breathing Halo
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .breathingGlow(color = GigColors.Primary, maxAlpha = 0.3f, durationMs = 2500)
                        .PulseGlow(
                            color = GigColors.Primary,
                            scaleMax = 1.05f,
                            durationMs = 2000
                        )
                        .background(
                            brush = Brush.linearGradient(listOf(GigColors.Primary, GigColors.Accent)),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(GigColors.SurfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = GigColors.PrimaryLight,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = user.name,
                    color = GigColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.75).sp
                )

                // Phase 4: Trust Badge
                user.trustBadge?.let { badge ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            badge.contains("Trusted") -> GigColors.Warning.copy(alpha = 0.15f)
                            badge.contains("Fast") -> GigColors.Primary.copy(alpha = 0.15f)
                            else -> GigColors.TextMuted.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = badge,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                badge.contains("Trusted") -> GigColors.Warning
                                badge.contains("Fast") -> GigColors.Primary
                                else -> GigColors.TextMuted
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Premium gradient underline
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    GigColors.Primary,
                                    GigColors.Accent,
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = GigColors.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = user.phone,
                        color = GigColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                GigStatusBadge(status = user.role.replaceFirstChar { it.uppercaseChar() })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Onboarding CTA (when no skills added) ──────────────────────
        if (uiState.editSkills.isEmpty()) {
            GigCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                glowColor = GigColors.Accent.copy(alpha = 0.1f)
            ) {
                // Progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Complete Your Profile",
                        color = GigColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = GigColors.Warning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "1/2",
                            color = GigColors.Warning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GigColors.Accent,
                    trackColor = GigColors.SurfaceHighest,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tips
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = GigColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Add your skills to unlock AI matching",
                            color = GigColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Our AI will match you with the best jobs based on your skills. The more you add, the better your matches!",
                            color = GigColors.TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GigGradientButton(
                    text = "Add Skills Now",
                    onClick = { viewModel.startEditing() },
                    icon = Icons.Filled.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // ─── Skills Section (when skills exist) ──────────────────────
            GigCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = GigColors.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Skills",
                            color = GigColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = GigColors.Success.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "2/2 ✓",
                                color = GigColors.Success,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${uiState.editSkills.size} skill${if (uiState.editSkills.size != 1) "s" else ""} • AI matching active",
                    color = GigColors.TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowSkills(skills = uiState.editSkills)

                // ─── AI Skill Recommendations ───
                if (!uiState.aiSkillRecommendations.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = GigColors.BorderSubtle)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoFixHigh, "AI", tint = GigColors.Accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✨ AI Suggested for you", color = GigColors.AccentLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.aiSkillRecommendations.take(3).forEach { rec ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GigColors.SurfaceHighest,
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.applyRecommendation(rec.skill) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("+ ${rec.skill}", color = GigColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            if (rec.newMatches > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(color = GigColors.PrimaryMuted, shape = CircleShape) {
                                                    Text("${rec.newMatches} jobs", color = GigColors.Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(rec.reason, color = GigColors.TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = GigColors.SuccessMuted,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "Add", color = GigColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── My Posted Gigs Section ─────────────────────────────────────
        if (uiState.myJobs.isNotEmpty()) {
            GigCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.WorkOutline,
                            contentDescription = null,
                            tint = GigColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Posted Gigs",
                            color = GigColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${uiState.myJobs.size} Gigs",
                        color = GigColors.TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show up to 3 most recent jobs
                uiState.myJobs.take(3).forEach { job ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = job.title,
                                color = GigColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Status: ${job.status.replaceFirstChar { it.uppercaseChar() }}",
                                color = GigColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        
                        Surface(
                            color = GigColors.PrimaryMuted,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${job.applicantCount ?: 0} applicants",
                                color = GigColors.PrimaryLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (job != uiState.myJobs.take(3).last()) {
                        HorizontalDivider(color = GigColors.BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

         // ─── Action Buttons ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            GigGradientButton(
                text = "Edit Profile",
                onClick = { viewModel.startEditing() },
                icon = Icons.Filled.Edit,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Logout button with confirmation
            var showLogoutDialog by remember { mutableStateOf(false) }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    containerColor = GigColors.SurfaceElevated,
                    titleContentColor = GigColors.TextPrimary,
                    textContentColor = GigColors.TextSecondary,
                    icon = {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = null,
                            tint = GigColors.Error,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = { Text("Sign Out?", fontWeight = FontWeight.Bold) },
                    text = { Text("You'll need to sign in again to apply for jobs and view your applications.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = GigColors.Error
                            )
                        ) {
                            Text("Sign Out", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLogoutDialog = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = GigColors.TextMuted
                            )
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GigColors.Error
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(
                        listOf(GigColors.Error.copy(alpha = 0.5f), GigColors.Error.copy(alpha = 0.2f))
                    )
                )
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── App Version Info ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = GigColors.BorderSubtle)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "StudentGig",
                color = GigColors.TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Version 1.0.0 • Made with ❤️",
                color = GigColors.TextMuted.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}


// ─── Edit Profile ───────────────────────────────────────────────────────────────

@Composable
private fun EditProfileContent(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // ─── Header ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GigGradients.HeaderGlow)
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Profile",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GigColors.TextPrimary,
                        letterSpacing = (-0.75).sp
                    )
                    Text(
                        text = "Update your info & skills",
                        fontSize = 13.sp,
                        color = GigColors.TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                IconButton(onClick = { viewModel.cancelEditing() }) {
                    Icon(Icons.Filled.Close, "Cancel", tint = GigColors.TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Name Field ─────────────────────────────────────────────────
        GigCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Display Name",
                color = GigColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            fun isJunkName(n: String): Boolean {
                if (n.isBlank()) return false
                val placeholders = listOf("admin", "test", "null", "none", "anonymous", "user", "unknown")
                if (n.lowercase() in placeholders) return true
                if (n.length >= 4 && n. windowed(4).any { win -> win.all { it == win[0] } }) return true
                return false
            }

            OutlinedTextField(
                value = uiState.editName,
                onValueChange = { 
                    if (it.length <= 50) viewModel.onNameChanged(it) 
                },
                placeholder = { Text("Your name") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, null, tint = GigColors.Primary)
                },
                singleLine = true,
                isError = uiState.editName.isNotEmpty() && (uiState.editName.any { it.isDigit() } || isJunkName(uiState.editName) || uiState.editName.length < 2),
                supportingText = {
                    when {
                        uiState.editName.isEmpty() -> Text("Name is required")
                        uiState.editName.any { it.isDigit() } -> Text("Professional names should not contain numbers")
                        isJunkName(uiState.editName) -> Text("Please enter a valid professional name")
                        uiState.editName.length < 2 -> Text("Name is too short")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GigColors.Primary,
                    unfocusedBorderColor = GigColors.Border,
                    cursorColor = GigColors.Primary,
                    focusedTextColor = GigColors.TextPrimary,
                    unfocusedTextColor = GigColors.TextPrimary,
                    focusedContainerColor = GigColors.SurfaceHighest,
                    unfocusedContainerColor = GigColors.SurfaceHighest,
                    focusedPlaceholderColor = GigColors.TextMuted,
                    unfocusedPlaceholderColor = GigColors.TextMuted,
                    errorSupportingTextColor = GigColors.Error,
                    errorBorderColor = GigColors.Error
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Skills Editor ──────────────────────────────────────────────
        GigCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = GigColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Skills",
                    color = GigColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Skills help our AI match you with the best jobs",
                color = GigColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Add skill input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.newSkillText,
                    onValueChange = { viewModel.onNewSkillTextChanged(it) },
                    placeholder = { Text("Add a skill…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.addSkill()
                        focusManager.clearFocus()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GigColors.Accent,
                        unfocusedBorderColor = GigColors.Border,
                        cursorColor = GigColors.Accent,
                        focusedTextColor = GigColors.TextPrimary,
                        unfocusedTextColor = GigColors.TextPrimary,
                        focusedContainerColor = GigColors.SurfaceHighest,
                        unfocusedContainerColor = GigColors.SurfaceHighest,
                        focusedPlaceholderColor = GigColors.TextMuted,
                        unfocusedPlaceholderColor = GigColors.TextMuted,
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                FilledIconButton(
                    onClick = { viewModel.addSkill() },
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GigColors.Accent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current skills
            if (uiState.editSkills.isNotEmpty()) {
                FlowSkillsEditable(
                    skills = uiState.editSkills,
                    onRemove = { viewModel.removeSkill(it) }
                )
            } else {
                Text(
                    text = "No skills added yet",
                    color = GigColors.TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Save Button ────────────────────────────────────────────────
        GigGradientButton(
            text = "Save Changes",
            onClick = { viewModel.saveProfile() },
            isLoading = uiState.isSaving,
            icon = Icons.Filled.Check,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}


// ─── Flow Layout for Skills (View Mode) ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowSkills(skills: List<String>) {
    FlowRow(
        modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEachIndexed { index, skill ->
            GigSkillChip(
                skill = skill,
                isHighlighted = true,
                modifier = Modifier.staggeredSlideIn(index, baseDelayMs = 50)
            )
        }
    }
}


// ─── Flow Layout for Skills (Edit Mode) ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowSkillsEditable(skills: List<String>, onRemove: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEachIndexed { index, skill ->
            InputChip(
                selected = false,
                onClick = { onRemove(skill) },
                label = {
                    Text(
                        text = skill,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GigColors.PrimaryLight
                    )
                },
                modifier = Modifier.cascadeEntrance(index = index, baseDelayMs = 40),
                trailingIcon = {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove $skill",
                        tint = GigColors.Error,
                        modifier = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = InputChipDefaults.inputChipColors(
                    containerColor = GigColors.PrimaryMuted,
                ),
                border = InputChipDefaults.inputChipBorder(
                    borderColor = GigColors.Primary.copy(alpha = 0.3f),
                    enabled = true,
                    selected = false
                )
            )
        }
    }
}
