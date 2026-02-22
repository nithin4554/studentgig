package com.studentgig.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.studentgig.app.ui.components.*
import com.studentgig.app.ui.theme.GigColors
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

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val msg = uiState.successMessage ?: uiState.errorMessage
        msg?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissMessage()
        }
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
            !uiState.isLoggedIn -> {
                NotLoggedInState()
            }
            uiState.isLoading -> {
                GigLoadingState(message = "Loading profile…")
            }
            uiState.isEditing -> {
                EditProfileContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding)
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
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}


// ─── Not Logged In ──────────────────────────────────────────────────────────────

@Composable
private fun NotLoggedInState() {
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
                    .size(100.dp)
                    .background(GigColors.PrimaryMuted, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PersonOff,
                    contentDescription = null,
                    tint = GigColors.Primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Not Signed In",
                color = GigColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Login from the Home tab to view and manage your profile",
                color = GigColors.TextMuted,
                fontSize = 14.sp,
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
                .background(GigGradients.HeaderGlow)
                .padding(top = 24.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(88.dp)
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.name,
                    color = GigColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                Spacer(modifier = Modifier.height(6.dp))
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
                            text = "Our AI will match you with the best gigs based on your skills. The more you add, the better your matches!",
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    text = { Text("You'll need to sign in again to apply for gigs and view your applications.") },
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

        Spacer(modifier = Modifier.height(40.dp))
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
            OutlinedTextField(
                value = uiState.editName,
                onValueChange = { viewModel.onNameChanged(it) },
                placeholder = { Text("Your name") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, null, tint = GigColors.Primary)
                },
                singleLine = true,
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
                text = "Skills help our AI match you with the best gigs",
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

        Spacer(modifier = Modifier.height(40.dp))
    }
}


// ─── Flow Layout for Skills (View Mode) ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowSkills(skills: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
            GigSkillChip(skill = skill, isHighlighted = true)
        }
    }
}


// ─── Flow Layout for Skills (Edit Mode) ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowSkillsEditable(skills: List<String>, onRemove: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
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
