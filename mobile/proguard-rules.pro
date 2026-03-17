# ═══════════════════════════════════════════════════════════════════════════════════
# StudentGig ProGuard / R8 Rules
# ═══════════════════════════════════════════════════════════════════════════════════

# ─── Retrofit + Gson ──────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*

# Keep Gson model classes (they use reflection for serialization)
-keep class com.studentgig.app.data.model.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── Hilt / Dagger ──────────────────────────────────────────────────────────────
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ─── Google Credentials / Identity ────────────────────────────────────────────
-dontwarn com.google.android.libraries.identity.**
-keep class com.google.android.libraries.identity.** { *; }

# ─── AndroidX Security Crypto ────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ─── Compose ──────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }
