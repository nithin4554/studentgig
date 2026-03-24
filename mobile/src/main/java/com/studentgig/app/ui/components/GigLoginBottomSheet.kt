package com.studentgig.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.viewmodel.LoginViewModel
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.studentgig.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GigLoginBottomSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = onDismiss,
    // Provide defaults for old calling convention (ignored)
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogin: (phone: String, name: String?) -> Unit = { _, _ -> },
    onGoogleLogin: (idToken: String) -> Unit = {},
    onFirebaseLogin: (idToken: String, name: String?) -> Unit = { _, _ -> },
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (uiState.isLoggedIn) onSuccess() else onDismiss()
        },
        sheetState = sheetState,
        containerColor = GigColors.SurfaceElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GigColors.TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.mode) {
                "login" -> {
                    Text("Welcome back", color = GigColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Text("Sign in with phone and password", color = GigColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = { viewModel.markTouched("phone"); viewModel.updateField("phone", it) },
                        label = { Text("Phone Number") },
                        isError = uiState.phoneError != null,
                        supportingText = { Text(uiState.phoneError ?: " ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("10 digits") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.markTouched("password"); viewModel.updateField("password", it) },
                        label = { Text("Password") },
                        isError = uiState.passwordError != null,
                        supportingText = { Text(uiState.passwordError ?: " ") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.updateField("mode", "forgot") }) {
                            Text("Forgot Password?", color = GigColors.Primary, fontSize = 13.sp)
                        }
                    }

                    if (uiState.errorMsg != null || uiState.successMsg != null) {
                        val msg = uiState.errorMsg ?: uiState.successMsg!!
                        val color = if (uiState.errorMsg != null) GigColors.Error else GigColors.Success
                        val bgColor = if (uiState.errorMsg != null) GigColors.ErrorMuted else GigColors.Background
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(16.dp))
                    } else Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.performLogin(onSuccess) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = GigColors.Primary)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Don't have an account? ", color = GigColors.TextSecondary, fontSize = 14.sp)
                        Text("Register", color = GigColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.updateField("mode", "register") })
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = GigColors.TextMuted.copy(alpha = 0.1f))
                        Text("OR", modifier = Modifier.padding(horizontal = 16.dp), color = GigColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = GigColors.TextMuted.copy(alpha = 0.1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Google Login Button
                    val credentialManager = remember { CredentialManager.create(context) }
                    val clientId = stringResource(id = R.string.default_web_client_id)
                    var isGoogleLoading by remember { mutableStateOf(false) }
                    var localGoogleError by remember { mutableStateOf<String?>(null) }

                    OutlinedButton(
                        onClick = {
                            if (isGoogleLoading) return@OutlinedButton
                            isGoogleLoading = true
                            localGoogleError = null
                            coroutineScope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(clientId)
                                        .setAutoSelectEnabled(false)
                                        .build()
                                        
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    
                                    val result = credentialManager.getCredential(context = context, request = request)
                                    val credential = result.credential
                                    
                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.performGoogleLogin(googleIdTokenCredential.idToken, onSuccess)
                                    } else {
                                        localGoogleError = "Google login failed"
                                    }
                                } catch (e: Exception) {
                                    localGoogleError = "Google login failed: ${e.localizedMessage}"
                                } finally {
                                    isGoogleLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GigColors.TextMuted.copy(alpha = 0.2f))
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GigColors.Primary, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G", fontWeight = FontWeight.Black, color = GigColors.Primary, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Continue with Google", color = GigColors.TextPrimary)
                            }
                        }
                    }
                    if (localGoogleError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(localGoogleError!!, color = GigColors.Error, fontSize = 13.sp)
                    }
                }
                "register" -> {
                    Text("Create Account", color = GigColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Choose your path", color = GigColors.TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))

                    // ─── Premium Role Selector ─────────────────────────────────
                    val isStudent = uiState.role == "student"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // --- Search Jobs card ---
                        val studentBorderColor by animateColorAsState(
                            targetValue = if (isStudent) GigColors.Primary else GigColors.TextMuted.copy(alpha = 0.15f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "studentBorder"
                        )
                        val studentBgColor by animateColorAsState(
                            targetValue = if (isStudent) GigColors.Primary.copy(alpha = 0.12f) else Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "studentBg"
                        )
                        val studentBarWidth by animateDpAsState(
                            targetValue = if (isStudent) 40.dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "studentBar"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, studentBorderColor, RoundedCornerShape(16.dp))
                                .background(studentBgColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.updateField("role", "student")
                                }
                                .padding(vertical = 18.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎓", fontSize = 28.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Search Jobs",
                                    color = if (isStudent) GigColors.TextPrimary else GigColors.TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.2).sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Find student jobs",
                                    color = if (isStudent) GigColors.TextSecondary else GigColors.TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                // Animated accent bar
                                Box(
                                    modifier = Modifier
                                        .width(studentBarWidth)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(GigColors.Primary, GigColors.Primary.copy(alpha = 0.5f))
                                            )
                                        )
                                )
                            }
                        }

                        // --- Hire Talent card ---
                        val employerBorderColor by animateColorAsState(
                            targetValue = if (!isStudent) GigColors.Accent else GigColors.TextMuted.copy(alpha = 0.15f),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "employerBorder"
                        )
                        val employerBgColor by animateColorAsState(
                            targetValue = if (!isStudent) GigColors.Accent.copy(alpha = 0.12f) else Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "employerBg"
                        )
                        val employerBarWidth by animateDpAsState(
                            targetValue = if (!isStudent) 40.dp else 0.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "employerBar"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, employerBorderColor, RoundedCornerShape(16.dp))
                                .background(employerBgColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.updateField("role", "employer")
                                }
                                .padding(vertical = 18.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💼", fontSize = 28.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Hire Talent",
                                    color = if (!isStudent) GigColors.TextPrimary else GigColors.TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.2).sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Post gigs & hire",
                                    color = if (!isStudent) GigColors.TextSecondary else GigColors.TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                // Animated accent bar
                                Box(
                                    modifier = Modifier
                                        .width(employerBarWidth)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(GigColors.Accent, GigColors.Accent.copy(alpha = 0.5f))
                                            )
                                        )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    val securityQuestions = remember {
                        listOf(
                            "What is your mother's maiden name?",
                            "What was the name of your first pet?",
                            "In what city were you born?",
                            "What was the make of your first car?",
                            "What was your childhood nickname?"
                        )
                    }
                    var expanded by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.markTouched("name"); viewModel.updateField("name", it) },
                        label = { Text("Full Name") },
                        isError = uiState.nameError != null,
                        supportingText = { Text(uiState.nameError ?: " ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                    )

                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = { viewModel.markTouched("phone"); viewModel.updateField("phone", it) },
                        label = { Text("Phone Number") },
                        isError = uiState.phoneError != null,
                        supportingText = { Text(uiState.phoneError ?: " ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("10 digits") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.markTouched("password"); viewModel.updateField("password", it) },
                        label = { Text("Create Password") },
                        isError = uiState.passwordError != null,
                        supportingText = { Text(uiState.passwordError ?: " ") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                    )
                    Spacer(Modifier.height(4.dp))

                    // Security Question Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.secQuestion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Security Question") },
                            isError = uiState.secQuestionError != null,
                            supportingText = { Text(uiState.secQuestionError ?: " ") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(GigColors.SurfaceElevated)
                        ) {
                            securityQuestions.forEach { question ->
                                DropdownMenuItem(
                                    text = { Text(question, color = GigColors.TextPrimary) },
                                    onClick = {
                                        viewModel.markTouched("secQuestion")
                                        viewModel.updateField("secQuestion", question)
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.secAnswer,
                        onValueChange = { viewModel.markTouched("secAnswer"); viewModel.updateField("secAnswer", it) },
                        label = { Text("Security Answer") },
                        isError = uiState.secAnswerError != null,
                        supportingText = { Text(uiState.secAnswerError ?: " ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                    )

                    if (uiState.errorMsg != null || uiState.successMsg != null) {
                        val msg = uiState.errorMsg ?: uiState.successMsg!!
                        val color = if (uiState.errorMsg != null) GigColors.Error else GigColors.Success
                        val bgColor = if (uiState.errorMsg != null) GigColors.ErrorMuted else GigColors.Background
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(16.dp))
                    } else Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.performRegister(onSuccess) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GigColors.Primary)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Already have an account? ", color = GigColors.TextSecondary, fontSize = 14.sp)
                        Text("Login", color = GigColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.updateField("mode", "login") })
                    }
                }
                "forgot" -> {
                    Text("Reset Password", color = GigColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(24.dp))

                    if (uiState.forgotStep == 1) {
                        OutlinedTextField(
                            value = uiState.phone,
                            onValueChange = { viewModel.markTouched("phone"); viewModel.updateField("phone", it) },
                            label = { Text("Phone Number") },
                            isError = uiState.phoneError != null,
                            supportingText = { Text(uiState.phoneError ?: " ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                        )
                        Spacer(Modifier.height(8.dp))
                        if (uiState.errorMsg != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(GigColors.ErrorMuted, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = GigColors.Error, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(uiState.errorMsg!!, color = GigColors.Error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        Button(
                            onClick = { viewModel.performGetResetQuestion() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !uiState.isLoading && uiState.phone.length > 5,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GigColors.Primary)
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Next", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Text("Question: ${uiState.fetchedQuestion}", color = GigColors.PrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.secAnswer,
                            onValueChange = { viewModel.markTouched("secAnswer"); viewModel.updateField("secAnswer", it) },
                            label = { Text("Your Answer") },
                            isError = uiState.secAnswerError != null,
                            supportingText = { Text(uiState.secAnswerError ?: " ") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                        )

                        OutlinedTextField(
                            value = uiState.newPassword,
                            onValueChange = { viewModel.markTouched("newPassword"); viewModel.updateField("newPassword", it) },
                            label = { Text("New Password") },
                            isError = uiState.newPasswordError != null,
                            supportingText = { Text(uiState.newPasswordError ?: " ") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GigColors.Primary, unfocusedBorderColor = GigColors.TextMuted.copy(alpha=0.2f), focusedLabelColor = GigColors.Primary, cursorColor = GigColors.Primary)
                        )

                        if (uiState.errorMsg != null || uiState.successMsg != null) {
                            val msg = uiState.errorMsg ?: uiState.successMsg!!
                            val color = if (uiState.errorMsg != null) GigColors.Error else GigColors.Success
                            val bgColor = if (uiState.errorMsg != null) GigColors.ErrorMuted else GigColors.Background
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(16.dp))
                        } else Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.performResetPassword({ /* handled in vm */ }) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !uiState.isLoading && uiState.secAnswer.isNotEmpty() && uiState.newPassword.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GigColors.Primary)
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.updateField("mode", "login") }) {
                        Text("Back to Login", color = GigColors.Primary)
                    }
                }
            }
        }
    }
}
