$content = Get-Content -Path "c:\Users\pardh\OneDrive\Desktop\StudentGig\mobile\src\main\java\com\studentgig\app\ui\components\GigComponents.kt" -Raw
$pattern = '(?s)@OptIn\(ExperimentalMaterial3Api::class\)\s+@Composable\s+fun GigLoginBottomSheet.*?^}'
$replacement = @"
enum class LoginStep { PHONE, OTP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GigLoginBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (phone: String, name: String?) -> Unit,
    onGoogleLogin: (idToken: String) -> Unit = {},
    onFirebaseLogin: (idToken: String, name: String?) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(LoginStep.PHONE) }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var localGoogleError by remember { mutableStateOf<String?>(null) }
    var storedVerificationId by remember { mutableStateOf("") }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    val isPhoneValid = phone.length == 10 && phone.all { it.isDigit() }
    val isOtpValid = otp.length == 6 && otp.all { it.isDigit() }
    val displayError = localGoogleError ?: errorMessage ?: otpError

    val context = LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }

    fun sendOtp() {
        if (!isPhoneValid) {
            phoneError = "Enter a valid 10-digit phone number"
            return
        }
        if (activity == null) {
            localGoogleError = "Activity context not found"
            return
        }
        isVerifyingOtp = true
        localGoogleError = null
        phoneError = null
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91`$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
                        if (task.isSuccessful) {
                            task.result?.user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    tokenTask.result?.token?.let { onFirebaseLogin(it, name.ifBlank { null }) }
                                }
                            }
                        }
                    }
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    localGoogleError = e.message
                    isVerifyingOtp = false
                }
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    storedVerificationId = verificationId
                    step = LoginStep.OTP
                    isVerifyingOtp = false
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        if (!isOtpValid) {
            otpError = "Enter 6-digit OTP"
            return
        }
        if (activity == null) return
        isVerifyingOtp = true
        otpError = null
        localGoogleError = null
        try {
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, otp)
            auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    task.result?.user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            tokenTask.result?.token?.let { onFirebaseLogin(it, name.ifBlank { null }) }
                        } else {
                            localGoogleError = tokenTask.exception?.message ?: "Failed to get token"
                            isVerifyingOtp = false
                        }
                    }
                } else {
                    otpError = task.exception?.message ?: "Invalid OTP"
                    isVerifyingOtp = false
                }
            }
        } catch (e: Exception) {
            otpError = e.message
            isVerifyingOtp = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(GigColors.PrimaryMuted, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = GigColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Welcome to StudentGig",
                        color = GigColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = if (step == LoginStep.PHONE) "Sign in to apply for jobs" else "We sent an OTP to +91 `$phone",
                        color = GigColors.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (step == LoginStep.PHONE) {
                // Phone Field
                Text(
                    text = "Phone Number",
                    color = GigColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        val digits = it.filter { c -> c.isDigit() }.take(10)
                        phone = digits
                        phoneError = when {
                            digits.isEmpty() -> null
                            digits.length < 10 -> "Enter a 10-digit phone number"
                            !digits.first().let { d -> d == '6' || d == '7' || d == '8' || d == '9' } ->
                                "Phone must start with 6, 7, 8 or 9"
                            else -> null
                        }
                        localGoogleError = null
                    },
                    placeholder = { Text("10-digit mobile number") },
                    leadingIcon = {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = GigColors.Primary)
                    },
                    isError = phoneError != null,
                    supportingText = if (phoneError != null) {
                        { Text(phoneError!!, color = GigColors.Error, fontSize = 11.sp) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (phoneError != null) GigColors.Error else GigColors.Primary,
                        unfocusedBorderColor = if (phoneError != null) GigColors.Error.copy(alpha = 0.5f) else GigColors.Border,
                        cursorColor = GigColors.Primary,
                        focusedTextColor = GigColors.TextPrimary,
                        unfocusedTextColor = GigColors.TextPrimary,
                        focusedContainerColor = GigColors.SurfaceHighest,
                        unfocusedContainerColor = GigColors.SurfaceHighest,
                        focusedPlaceholderColor = GigColors.TextMuted,
                        unfocusedPlaceholderColor = GigColors.TextMuted,
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Name Field
                Text(
                    text = "Your Name (optional)",
                    color = GigColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localGoogleError = null
                    },
                    placeholder = { Text("Enter your name") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = GigColors.Accent)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isPhoneValid && phoneError == null) sendOtp()
                        }
                    ),
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
            } else {
                // OTP Field
                Text(
                    text = "Enter OTP",
                    color = GigColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = otp,
                    onValueChange = {
                        val digits = it.filter { c -> c.isDigit() }.take(6)
                        otp = digits
                        otpError = null
                        localGoogleError = null
                    },
                    placeholder = { Text("6-digit code") },
                    leadingIcon = {
                        Icon(Icons.Filled.Password, contentDescription = null, tint = GigColors.Primary)
                    },
                    isError = otpError != null,
                    supportingText = if (otpError != null) {
                        { Text(otpError!!, color = GigColors.Error, fontSize = 11.sp) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { verifyOtp() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (otpError != null) GigColors.Error else GigColors.Primary,
                        unfocusedBorderColor = if (otpError != null) GigColors.Error.copy(alpha = 0.5f) else GigColors.Border,
                        cursorColor = GigColors.Primary,
                        focusedTextColor = GigColors.TextPrimary,
                        unfocusedTextColor = GigColors.TextPrimary,
                        focusedContainerColor = GigColors.SurfaceHighest,
                        unfocusedContainerColor = GigColors.SurfaceHighest,
                        focusedPlaceholderColor = GigColors.TextMuted,
                        unfocusedPlaceholderColor = GigColors.TextMuted,
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { step = LoginStep.PHONE; otp = "" }) {
                        Text("Change Number", fontSize = 12.sp, color = GigColors.Primary)
                    }
                    TextButton(onClick = { sendOtp() }, enabled = !isVerifyingOtp && !isLoading) {
                        Text("Resend OTP", fontSize = 12.sp, color = GigColors.Primary)
                    }
                }
            }

            // error message
            if (displayError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GigColors.ErrorMuted, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = GigColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayError,
                        color = GigColors.Error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val currentActionLoading = isLoading || isVerifyingOtp

            // Submit Button
            GigGradientButton(
                text = if (step == LoginStep.PHONE) "Send OTP" else "Verify OTP",
                onClick = { if (step == LoginStep.PHONE) sendOtp() else verifyOtp() },
                enabled = if (step == LoginStep.PHONE) isPhoneValid && phoneError == null else isOtpValid && otpError == null,
                isLoading = currentActionLoading,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )

            if (step == LoginStep.PHONE) {
                Spacer(modifier = Modifier.height(16.dp))

                // Or separator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = GigColors.Border)
                    Text(
                        "OR",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        fontSize = 12.sp,
                        color = GigColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = GigColors.Border)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Google Login Button
                val coroutineScope = rememberCoroutineScope()
                val credentialManager = remember { CredentialManager.create(context) }
                val clientId = stringResource(id = R.string.default_web_client_id)
                var isGoogleLoading by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        if (isGoogleLoading || currentActionLoading) return@Button
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
                                
                                val result = credentialManager.getCredential(
                                    context = context,
                                    request = request
                                )
                                
                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    onGoogleLogin(googleIdTokenCredential.idToken)
                                } else {
                                    Log.e("GoogleLogin", "Unexpected credential type: `${credential.type}")
                                    localGoogleError = "Google login failed: unexpected response"
                                }
                            } catch (e: Exception) {
                                Log.e("GoogleLogin", "Error getting credential", e)
                                localGoogleError = "Google login failed: `${e.localizedMessage}"
                            } finally {
                                isGoogleLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GigColors.SurfaceHighest,
                        contentColor = GigColors.TextPrimary
                    )
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = GigColors.Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "G",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GigColors.Primary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Continue with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GigColors.TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy note
            Text(
                text = "By continuing, you agree to our Terms of Service",
                color = GigColors.TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
"@

$newContent = [regex]::Replace($content, $pattern, $replacement, 'Singleline')
Set-Content -Path "c:\Users\pardh\OneDrive\Desktop\StudentGig\mobile\src\main\java\com\studentgig\app\ui\components\GigComponents.kt" -Value $newContent -Encoding UTF8
