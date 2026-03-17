package com.studentgig.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.studentgig.app.ui.theme.StudentGigTheme
import com.studentgig.app.ui.navigation.StudentGigNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkJobId = parseDeepLinkJobId(intent)

        setContent {
            StudentGigTheme {
                StudentGigNavHost(deepLinkJobId = deepLinkJobId)
            }
        }
    }

    /**
     * Parse a deep link intent to extract a job ID.
     * Supports: studentgig://job/{id} and https://studentgig.com/job/{id}
     */
    private fun parseDeepLinkJobId(intent: Intent?): Int? {
        val uri = intent?.data ?: return null
        return when {
            // studentgig://job/{id} → pathSegments = ["{id}"]
            uri.scheme == "studentgig" && uri.host == "job" ->
                uri.pathSegments?.firstOrNull()?.toIntOrNull()
            // https://studentgig.com/job/{id} → pathSegments = ["job", "{id}"]
            uri.host == "studentgig.com" && uri.pathSegments?.firstOrNull() == "job" ->
                uri.pathSegments?.getOrNull(1)?.toIntOrNull()
            else -> null
        }
    }
}
