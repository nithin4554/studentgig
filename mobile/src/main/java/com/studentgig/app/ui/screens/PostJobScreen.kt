package com.studentgig.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentgig.app.ui.theme.GigColors
import com.studentgig.app.ui.theme.GigGlow
import com.studentgig.app.ui.theme.GigGradients
import com.studentgig.app.ui.components.GigLoginBottomSheet
import com.studentgig.app.ui.viewmodel.PostJobViewModel
import com.studentgig.app.data.model.AIGenerateDescriptionResponse
import com.studentgig.app.data.model.AIPayEstimateResponse
import kotlinx.coroutines.delay
import java.util.Calendar


// ─── Category Icons Mapping ──────────────────────────────────────────────────
private fun categoryIcon(category: String): ImageVector = when (category) {
    "Tutoring" -> Icons.Filled.School
    "Delivery" -> Icons.Filled.LocalShipping
    "Events" -> Icons.Filled.Celebration
    "Tech" -> Icons.Filled.Computer
    "Content Creation" -> Icons.Filled.Edit
    "Design" -> Icons.Filled.Palette
    "Marketing" -> Icons.Filled.Campaign
    "Data Entry" -> Icons.Filled.Storage
    "Photography" -> Icons.Filled.CameraAlt
    "Volunteering" -> Icons.Filled.VolunteerActivism
    "Writing" -> Icons.Filled.Description
    "Translation" -> Icons.Filled.Translate
    "Hospitality" -> Icons.Filled.Restaurant
    "Fitness" -> Icons.Filled.FitnessCenter
    else -> Icons.Filled.Work
}

// ─── Skills suggestions per category ─────────────────────────────────────────
private fun suggestedSkills(category: String): List<String> = when (category) {
    "Tutoring" -> listOf("Mathematics", "Physics", "Chemistry", "English", "Hindi", "Teaching", "Patience", "NCERT", "Python", "Java")
    "Delivery" -> listOf("Bicycle", "Bike", "Punctuality", "Navigation", "Fitness", "Communication")
    "Events" -> listOf("Communication", "Teamwork", "Public Speaking", "Hindi", "English", "Management", "Anchoring")
    "Tech" -> listOf("Python", "Java", "JavaScript", "React", "Android", "iOS", "SQL", "HTML/CSS", "Git", "APIs")
    "Content Creation" -> listOf("Instagram", "YouTube", "Canva", "Writing", "Editing", "Reels", "SEO", "Copywriting")
    "Design" -> listOf("Canva", "Figma", "Photoshop", "Illustrator", "UI/UX", "Logo Design", "Branding")
    "Marketing" -> listOf("Social Media", "Communication", "Leadership", "SEO", "Content", "Campaigns", "Analytics")
    "Data Entry" -> listOf("Excel", "Typing", "Accuracy", "Google Sheets", "Data Analysis", "Attention to Detail")
    "Photography" -> listOf("DSLR", "Editing", "Lightroom", "Photoshop", "Portraits", "Event Photography")
    "Writing" -> listOf("English", "Hindi", "Blog Writing", "Articles", "Research", "Grammar", "SEO")
    "Translation" -> listOf("Hindi", "English", "Telugu", "Tamil", "Kannada", "Bengali", "Marathi")
    "Hospitality" -> listOf("Cooking", "Serving", "Communication", "Cleanliness", "Teamwork", "Hindi", "English")
    "Fitness" -> listOf("Yoga", "Gym Training", "Nutrition", "CPR", "First Aid", "Communication")
    "Volunteering" -> listOf("Teamwork", "Communication", "Hindi", "English", "Leadership", "Social Work")
    else -> listOf("Communication", "English", "Hindi", "Teamwork", "Computer", "Typing")
}

// ─── Pay presets ─────────────────────────────────────────────────────────────
private val PAY_PRESETS = listOf("₹300", "₹500", "₹800", "₹1000", "₹1500", "₹2000", "₹3000", "₹5000")
private fun payPresetValue(label: String) = label.removePrefix("₹").toDoubleOrNull() ?: 0.0

// ─── Location presets ────────────────────────────────────────────────────────
private val LOCATION_PRESETS = listOf("Remote", "Hyderabad", "Bangalore", "Mumbai", "Delhi", "Pune", "Chennai", "Kolkata")

// ─── Duration presets ────────────────────────────────────────────────────────
private val DURATION_PRESETS = listOf("1 hour", "2 hours", "3 hours", "Half day", "Full day", "2 days", "1 week", "Ongoing")


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    onBack: () -> Unit = {},
    onJobPosted: () -> Unit = {},
    viewModel: PostJobViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // No auto-dismiss anymore, we will let the user click "Share" or "Done"
    // on the SuccessOverlay.

    // Login sheet
    if (state.showLoginSheet) {
        GigLoginBottomSheet(
            onDismiss = { viewModel.dismissLoginSheet() },
            onSuccess = { viewModel.dismissLoginSheet() }
        )
    }

    Scaffold(
        containerColor = GigColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Post a Job",
                        color = GigColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GigColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GigColors.Background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isSuccess) {
                SuccessOverlay(
                    jobTitle = state.title,
                    onDone = onJobPosted
                )
            } else if (!state.isLoggedIn) {
                // ─── Login Gate ────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = GigColors.Primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Sign in to Post a Job",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GigColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You need to be signed in to post jobs and manage applicants",
                        fontSize = 14.sp,
                        color = GigColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.showLoginSheet() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GigColors.Primary
                        )
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Step indicator
                    StepProgressBar(
                        currentStep = state.currentStep,
                        totalSteps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step content
                    AnimatedContent(
                        targetState = state.currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { it } + fadeIn()) togetherWith
                                        (slideOutHorizontally { -it } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()) togetherWith
                                        (slideOutHorizontally { it } + fadeOut())
                            }
                        },
                        label = "stepTransition"
                    ) { step ->
                        when (step) {
                            0 -> Step1WhatAndWhere(
                                title = state.title,
                                onTitleChange = viewModel::setTitle,
                                titleError = state.titleError,
                                category = state.category,
                                onCategoryChange = viewModel::setCategory,
                                categoryError = state.categoryError,
                                categories = state.categories,
                                location = state.location,
                                onLocationChange = viewModel::setLocation,
                                locationError = state.locationError
                            )
                            1 -> Step2PayAndSchedule(
                                payAmount = state.payAmount,
                                onPayChange = viewModel::setPayAmount,
                                payError = state.payError,
                                jobType = state.jobType,
                                onJobTypeChange = viewModel::setJobType,
                                jobTypes = state.jobTypes,
                                duration = state.duration,
                                onDurationChange = viewModel::setDuration,
                                isUrgent = state.isUrgent,
                                onUrgentChange = viewModel::setIsUrgent,
                                jobDate = state.jobDate,
                                onJobDateChange = viewModel::setJobDate,
                                startTime = state.startTime,
                                onStartTimeChange = viewModel::setStartTime,
                                endTime = state.endTime,
                                onEndTimeChange = viewModel::setEndTime,
                                context = context,
                                isEstimatingPay = state.isEstimatingPay,
                                aiPayData = state.aiPayData,
                                onEstimatePay = viewModel::estimatePay,
                                onApplyPay = viewModel::applyEstimatedPay,
                                onDismissPay = viewModel::dismissAiPayData
                            )
                            2 -> Step3SkillsAndPost(
                                category = state.category,
                                skillsRequired = state.skillsRequired,
                                onSkillsChange = viewModel::setSkillsRequired,
                                description = state.description,
                                onDescriptionChange = viewModel::setDescription,
                                companyName = state.companyName,
                                onCompanyNameChange = viewModel::setCompanyName,
                                contactInfo = state.contactInfo,
                                onContactInfoChange = viewModel::setContactInfo,
                                address = state.address,
                                onAddressChange = viewModel::setAddress,
                                maxApplicants = state.maxApplicants,
                                onMaxApplicantsChange = viewModel::setMaxApplicants,
                                state = state,
                                isGeneratingDescription = state.isGeneratingDescription,
                                aiDescriptionData = state.aiDescriptionData,
                                onGenerateDescription = viewModel::generateDescription,
                                onApplyDescriptionSkills = viewModel::applySuggestedSkillsAndCategory,
                                onDismissDescription = viewModel::dismissAiDescriptionData
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error message
                    state.errorMessage?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GigColors.ErrorMuted,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = GigColors.Error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = error, color = GigColors.Error, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (state.currentStep > 0) {
                            OutlinedButton(
                                onClick = { viewModel.previousStep() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GigColors.TextSecondary),
                                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                    brush = Brush.horizontalGradient(listOf(GigColors.Border, GigColors.Border))
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                if (state.currentStep == 2) {
                                    viewModel.postJob()
                                } else {
                                    viewModel.nextStep()
                                }
                            },
                            enabled = !state.isPosting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.currentStep == 2) GigColors.Success else GigColors.Primary
                            ),
                            modifier = Modifier
                                .height(52.dp)
                                .then(
                                    if (state.currentStep == 2) Modifier.fillMaxWidth(0.6f) else Modifier
                                )
                        ) {
                            if (state.isPosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Posting...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Text(
                                    text = if (state.currentStep == 2) "🚀 Post Job" else "Next",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                if (state.currentStep < 2) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
//  STEP PROGRESS BAR — Now 3 steps
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StepProgressBar(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    val stepLabels = listOf("What & Where", "Pay & When", "Details & Post")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalSteps) {
                val isCompleted = i < currentStep
                val isCurrent = i == currentStep

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> GigColors.Success
                                isCurrent -> GigColors.Primary
                                else -> GigColors.SurfaceHighest
                            }
                        )
                        .then(
                            if (isCurrent) Modifier.border(2.dp, GigColors.PrimaryLight, CircleShape) else Modifier
                        )
                ) {
                    if (isCompleted) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = "${i + 1}",
                            color = if (isCurrent) Color.White else GigColors.TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (i < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i < currentStep) GigColors.Success.copy(alpha = 0.6f) else GigColors.SurfaceHighest
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stepLabels.getOrElse(currentStep) { "" },
            color = GigColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
//  STEP 1: What & Where — Title (type) + Category (tap) + Location (tap)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun Step1WhatAndWhere(
    title: String,
    onTitleChange: (String) -> Unit,
    titleError: String?,
    category: String,
    onCategoryChange: (String) -> Unit,
    categoryError: String?,
    categories: List<String>,
    location: String,
    onLocationChange: (String) -> Unit,
    locationError: String?
) {
    Column {
        SectionHeader(
            icon = Icons.Filled.WorkOutline,
            title = "What's the job?",
            subtitle = "Title, category, and location — that's all"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Job title — MUST type (no alternative)
        PremiumTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "Job Title *",
            placeholder = "e.g. Python Tutor for Beginners",
            icon = Icons.Outlined.Title,
            error = titleError,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Category — TAP to select
        Text("Category *", color = GigColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        val rows = categories.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { cat ->
                    val isSelected = category == cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) GigColors.PrimaryMuted else GigColors.SurfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryChange(cat) }
                            .then(
                                if (isSelected) Modifier.border(1.dp, GigColors.Primary, RoundedCornerShape(12.dp))
                                else Modifier.border(1.dp, GigColors.BorderSubtle, RoundedCornerShape(12.dp))
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon(cat),
                                contentDescription = cat,
                                tint = if (isSelected) GigColors.Primary else GigColors.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cat,
                                color = if (isSelected) GigColors.Primary else GigColors.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        categoryError?.let {
            Text(it, color = GigColors.Error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Location — TAP preset or type custom
        Text("Location *", color = GigColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        // Location preset chips (2 rows of 4)
        val locationRows = LOCATION_PRESETS.chunked(4)
        locationRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { loc ->
                    val isSelected = location == loc
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) GigColors.PrimaryMuted else GigColors.SurfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLocationChange(loc) }
                            .border(
                                1.dp,
                                if (isSelected) GigColors.Primary else GigColors.BorderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Text(
                            text = loc,
                            color = if (isSelected) GigColors.Primary else GigColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Custom location if not in presets
        if (location.isNotBlank() && location !in LOCATION_PRESETS) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GigColors.PrimaryMuted,
                modifier = Modifier.border(1.dp, GigColors.Primary, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = "📍 $location",
                    color = GigColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // "Other" option — type custom
        PremiumTextField(
            value = if (location in LOCATION_PRESETS) "" else location,
            onValueChange = onLocationChange,
            label = "Or type a city",
            placeholder = "Type if not listed above",
            icon = Icons.Outlined.LocationOn,
            error = locationError,
            singleLine = true
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
//  STEP 2: Pay & Schedule — All tappable chips + date/time pickers
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun Step2PayAndSchedule(
    payAmount: String,
    onPayChange: (String) -> Unit,
    payError: String?,
    jobType: String,
    onJobTypeChange: (String) -> Unit,
    jobTypes: List<String>,
    duration: String,
    onDurationChange: (String) -> Unit,
    isUrgent: Boolean,
    onUrgentChange: (Boolean) -> Unit,
    jobDate: String,
    onJobDateChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    context: android.content.Context,
    isEstimatingPay: Boolean,
    aiPayData: AIPayEstimateResponse?,
    onEstimatePay: () -> Unit,
    onApplyPay: () -> Unit,
    onDismissPay: () -> Unit
) {
    Column {
        SectionHeader(
            icon = Icons.Filled.Payments,
            title = "Pay & Schedule",
            subtitle = "How much and when — just tap to select"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Pay — TAP presets ───────────────────────────────────────────
        Text("Pay Amount (₹) *", color = GigColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        val payRows = PAY_PRESETS.chunked(4)
        payRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { preset ->
                    val presetVal = payPresetValue(preset).toInt().toString()
                    val isSelected = payAmount == presetVal
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) GigColors.SuccessMuted else GigColors.SurfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPayChange(presetVal) }
                            .border(
                                1.dp,
                                if (isSelected) GigColors.Success else GigColors.BorderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Text(
                            text = preset,
                            color = if (isSelected) GigColors.Success else GigColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Custom pay input
        PremiumTextField(
            value = if (PAY_PRESETS.any { payPresetValue(it).toInt().toString() == payAmount }) "" else payAmount,
            onValueChange = onPayChange,
            label = "Or enter custom amount",
            placeholder = "e.g. 750",
            icon = Icons.Outlined.CurrencyRupee,
            error = payError,
            singleLine = true,
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ✨ AI Pay Estimator
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GigColors.Primary.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEstimatePay() }
                .border(1.dp, GigColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, "AI", tint = GigColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Unsure about the pay?", color = GigColors.PrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Get an AI estimate based on market rates", color = GigColors.TextSecondary, fontSize = 11.sp)
                }
                if (isEstimatingPay) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GigColors.Primary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.ArrowForwardIos, null, tint = GigColors.Primary, modifier = Modifier.size(14.dp))
                }
            }
        }
        
        if (aiPayData != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GigColors.SuccessMuted,
                modifier = Modifier.fillMaxWidth().border(1.dp, GigColors.Success.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("✨ AI Suggests: ₹${aiPayData.avgPay?.toInt() ?: 0}", color = GigColors.Success, fontWeight = FontWeight.Bold)
                        Text(aiPayData.reasoning, color = GigColors.TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApplyPay(); onDismissPay() },
                        colors = ButtonDefaults.buttonColors(containerColor = GigColors.Success),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Job Type — TAP ──────────────────────────────────────────────
        Text("Job Type", color = GigColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            jobTypes.forEach { type ->
                val isSelected = jobType == type
                val label = when (type) {
                    "one-time" -> "One-Time"
                    "part-time" -> "Part-Time"
                    "recurring" -> "Recurring"
                    else -> type
                }
                val icon = when (type) {
                    "one-time" -> Icons.Outlined.Bolt
                    "part-time" -> Icons.Outlined.Schedule
                    "recurring" -> Icons.Outlined.Repeat
                    else -> Icons.Outlined.Work
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) GigColors.PrimaryMuted else GigColors.SurfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onJobTypeChange(type) }
                        .border(
                            1.dp,
                            if (isSelected) GigColors.Primary else GigColors.BorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) GigColors.Primary else GigColors.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = if (isSelected) GigColors.Primary else GigColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Duration — TAP presets ──────────────────────────────────────
        Text("Duration", color = GigColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        val durationRows = DURATION_PRESETS.chunked(4)
        durationRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { d ->
                    val isSelected = duration == d
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) GigColors.AccentMuted else GigColors.SurfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDurationChange(d) }
                            .border(
                                1.dp,
                                if (isSelected) GigColors.AccentLight else GigColors.BorderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Text(
                            text = d,
                            color = if (isSelected) GigColors.AccentLight else GigColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(vertical = 9.dp, horizontal = 2.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Date & Time pickers (tappable cards) ────────────────────────
        Text("📅 Schedule (optional)", color = GigColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Tap to set a specific date and time", color = GigColors.TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Date picker card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = GigColors.SurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onJobDateChange(String.format("%04d-%02d-%02d", year, month + 1, day))
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        datePicker.minDate = System.currentTimeMillis() - 1000
                    }.show()
                }
                .border(1.dp, GigColors.BorderSubtle, RoundedCornerShape(14.dp))
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarToday, contentDescription = null,
                    tint = if (jobDate.isNotBlank()) GigColors.Primary else GigColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (jobDate.isNotBlank()) "📅 $jobDate" else "Tap to pick date (flexible if empty)",
                    color = if (jobDate.isNotBlank()) GigColors.TextPrimary else GigColors.TextMuted,
                    fontSize = 14.sp,
                    fontWeight = if (jobDate.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (jobDate.isNotBlank()) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "Clear",
                        tint = GigColors.TextMuted, modifier = Modifier
                            .size(18.dp)
                            .clickable { onJobDateChange("") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Time pickers row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Start time
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GigColors.SurfaceElevated,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        TimePickerDialog(context, { _, h, m ->
                            onStartTimeChange(String.format("%02d:%02d", h, m))
                        }, 9, 0, true).show()
                    }
                    .border(1.dp, GigColors.BorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null,
                        tint = if (startTime.isNotBlank()) GigColors.Success else GigColors.TextMuted,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Start", color = GigColors.TextMuted, fontSize = 11.sp)
                        Text(
                            text = if (startTime.isNotBlank()) startTime else "--:--",
                            color = if (startTime.isNotBlank()) GigColors.TextPrimary else GigColors.TextMuted,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // End time
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GigColors.SurfaceElevated,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        TimePickerDialog(context, { _, h, m ->
                            onEndTimeChange(String.format("%02d:%02d", h, m))
                        }, 17, 0, true).show()
                    }
                    .border(1.dp, GigColors.BorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null,
                        tint = if (endTime.isNotBlank()) GigColors.Error else GigColors.TextMuted,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("End", color = GigColors.TextMuted, fontSize = 11.sp)
                        Text(
                            text = if (endTime.isNotBlank()) endTime else "--:--",
                            color = if (endTime.isNotBlank()) GigColors.TextPrimary else GigColors.TextMuted,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Urgent toggle
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isUrgent) GigColors.ErrorMuted else GigColors.SurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUrgentChange(!isUrgent) }
                .border(
                    1.dp,
                    if (isUrgent) GigColors.Error.copy(alpha = 0.5f) else GigColors.BorderSubtle,
                    RoundedCornerShape(14.dp)
                )
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PriorityHigh, contentDescription = null,
                    tint = if (isUrgent) GigColors.Error else GigColors.TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("🔥 Urgent", color = GigColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Gets highlighted and shown first", color = GigColors.TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = isUrgent,
                    onCheckedChange = onUrgentChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GigColors.Error,
                        uncheckedThumbColor = GigColors.TextMuted,
                        uncheckedTrackColor = GigColors.SurfaceHighest
                    )
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
//  STEP 3: Skills (tap chips) + Optional extras + Live Preview
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun Step3SkillsAndPost(
    category: String,
    skillsRequired: String,
    onSkillsChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    contactInfo: String,
    onContactInfoChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    maxApplicants: String,
    onMaxApplicantsChange: (String) -> Unit,
    state: com.studentgig.app.ui.viewmodel.PostJobUiState,
    isGeneratingDescription: Boolean,
    aiDescriptionData: AIGenerateDescriptionResponse?,
    onGenerateDescription: () -> Unit,
    onApplyDescriptionSkills: () -> Unit,
    onDismissDescription: () -> Unit
) {
    // Parse current skills
    val selectedSkills = remember(skillsRequired) {
        if (skillsRequired.isBlank()) emptyList()
        else skillsRequired.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val suggested = remember(category) { suggestedSkills(category) }

    Column {
        SectionHeader(
            icon = Icons.Filled.Psychology,
            title = "Skills & Details",
            subtitle = "Tap skills to add — then review and post!"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✨ AI Description Generator
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GigColors.Accent.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGenerateDescription() }
                .border(1.dp, GigColors.Accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoFixHigh, "AI", tint = GigColors.Accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-write with AI", color = GigColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Generates a clear description based on your title", color = GigColors.TextSecondary, fontSize = 12.sp)
                }
                if (isGeneratingDescription) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GigColors.Accent, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.ArrowForwardIos, null, tint = GigColors.Accent, modifier = Modifier.size(14.dp))
                }
            }
        }
        
        if (aiDescriptionData != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GigColors.SuccessMuted,
                modifier = Modifier.border(1.dp, GigColors.Success.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("✨ Generated & Applied Description:", color = GigColors.Success, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(aiDescriptionData.description, color = GigColors.TextPrimary, fontSize = 13.sp)
                    
                    if (!aiDescriptionData.suggestedSkills.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Lightbulb, null, tint = GigColors.Warning, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suggested Skills: ${aiDescriptionData.suggestedSkills}", color = GigColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            
                            TextButton(
                                onClick = { onApplyDescriptionSkills(); onDismissDescription() },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Add Skills", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Skills — TAP chips ──────────────────────────────────────────
        Text("Required Skills (tap to select)", color = GigColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        // Suggested skills grid (based on category)
        val skillRows = suggested.chunked(3)
        skillRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { skill ->
                    val isSelected = skill in selectedSkills
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) GigColors.PrimaryMuted else GigColors.SurfaceElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val newSkills = if (isSelected) {
                                    selectedSkills.filter { it != skill }
                                } else {
                                    selectedSkills + skill
                                }
                                onSkillsChange(newSkills.joinToString(", "))
                            }
                            .border(
                                1.dp,
                                if (isSelected) GigColors.Primary else GigColors.BorderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check, contentDescription = null,
                                    tint = GigColors.Primary, modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = skill,
                                color = if (isSelected) GigColors.Primary else GigColors.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Selected count
        if (selectedSkills.isNotEmpty()) {
            Text(
                text = "✅ ${selectedSkills.size} skills selected",
                color = GigColors.Success,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Max Applicants — TAP chips ──────────────────────────────────
        Text("How many people needed?", color = GigColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1", "2", "3", "5", "10").forEach { count ->
                val isSelected = maxApplicants == count
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) GigColors.PrimaryMuted else GigColors.SurfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onMaxApplicantsChange(count) }
                        .border(
                            1.dp,
                            if (isSelected) GigColors.Primary else GigColors.BorderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Text(
                        text = count,
                        color = if (isSelected) GigColors.Primary else GigColors.TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Optional extras (collapsible) ───────────────────────────────
        var showExtras by remember { mutableStateOf(false) }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GigColors.SurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showExtras = !showExtras }
                .border(1.dp, GigColors.BorderSubtle, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Tune, contentDescription = null,
                    tint = GigColors.TextMuted, modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add more details (optional)",
                    color = GigColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (showExtras) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = GigColors.TextMuted
                )
            }
        }

        AnimatedVisibility(visible = showExtras) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                PremiumTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = "Description",
                    placeholder = "What will the person be doing?",
                    icon = Icons.Outlined.Notes,
                    singleLine = false,
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                PremiumTextField(
                    value = companyName,
                    onValueChange = onCompanyNameChange,
                    label = "Company / Your Name",
                    placeholder = "e.g. TechCorp",
                    icon = Icons.Outlined.Business,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                PremiumTextField(
                    value = contactInfo,
                    onValueChange = onContactInfoChange,
                    label = "Contact Info",
                    placeholder = "Phone or email",
                    icon = Icons.Outlined.ContactPhone,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                PremiumTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = "Full Address",
                    placeholder = "Exact location for the job",
                    icon = Icons.Outlined.Place,
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Live Preview Card ───────────────────────────────────────────
        Text("📋 Preview", color = GigColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GigColors.SurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GigColors.Border, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.title.ifBlank { "Untitled Job" },
                        color = GigColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.isUrgent) {
                        Surface(shape = RoundedCornerShape(6.dp), color = GigColors.ErrorMuted) {
                            Text("🔥", fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.category.isNotBlank()) {
                        PreviewChip("📂 ${state.category}")
                    }
                    PreviewChip("₹${state.payAmount.ifBlank { "0" }}")
                    PreviewChip("📍 ${state.location.ifBlank { "—" }}")
                }

                if (state.jobDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreviewChip("📅 ${state.jobDate}")
                        if (state.startTime.isNotBlank() && state.endTime.isNotBlank()) {
                            PreviewChip("⏰ ${state.startTime}-${state.endTime}")
                        }
                    }
                }

                if (state.duration.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    PreviewChip("⏱ ${state.duration}")
                }

                if (selectedSkills.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        selectedSkills.take(4).forEach { skill ->
                            Surface(shape = RoundedCornerShape(6.dp), color = GigColors.PrimaryMuted) {
                                Text(
                                    text = skill,
                                    color = GigColors.PrimaryLight,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (selectedSkills.size > 4) {
                            Text("+${selectedSkills.size - 4}", color = GigColors.TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
//  REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PreviewChip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = GigColors.SurfaceHighest) {
        Text(
            text = text,
            color = GigColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}


@Composable
private fun SectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = GigColors.PrimaryMuted,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = icon, contentDescription = null, tint = GigColors.Primary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, color = GigColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = GigColors.TextMuted, fontSize = 13.sp)
        }
    }
}


@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: ImageVector? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = GigColors.TextMuted.copy(alpha = 0.5f), fontSize = 13.sp) },
        leadingIcon = icon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = GigColors.TextMuted, modifier = Modifier.size(20.dp)) }
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = error != null,
        supportingText = error?.let { { Text(it, color = GigColors.Error, fontSize = 11.sp) } },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GigColors.TextPrimary,
            unfocusedTextColor = GigColors.TextPrimary,
            cursorColor = GigColors.Primary,
            focusedBorderColor = GigColors.Primary,
            unfocusedBorderColor = GigColors.Border,
            errorBorderColor = GigColors.Error,
            focusedLabelColor = GigColors.Primary,
            unfocusedLabelColor = GigColors.TextMuted,
            focusedContainerColor = GigColors.SurfaceElevated,
            unfocusedContainerColor = GigColors.SurfaceElevated,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}


// ═══════════════════════════════════════════════════════════════════════════════
//  SUCCESS OVERLAY
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SuccessOverlay(jobTitle: String, onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "successScale"
    )
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GigColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .padding(40.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = GigColors.SuccessMuted,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = GigColors.Success,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Job Posted! 🎉",
                color = GigColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"$jobTitle\"",
                color = GigColors.Primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Students can now see and apply for your job",
                color = GigColors.TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            // Phase 5: Post-posting nudge
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GigColors.Accent.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Get applicants faster!",
                        fontWeight = FontWeight.Bold,
                        color = GigColors.Accent,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Share with your college WhatsApp groups:",
                        color = GigColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Hey! I just posted a gig on StudentGig: \"$jobTitle\". Apply now if you're interested! 🚀")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Job via..."))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GigColors.Accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Job", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onDone) {
                Text("Done", color = GigColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
