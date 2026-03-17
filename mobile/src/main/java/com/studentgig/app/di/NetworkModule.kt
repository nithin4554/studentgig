package com.studentgig.app.di

import com.studentgig.app.data.local.TokenManager
import com.studentgig.app.data.remote.JobApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.studentgig.app.BuildConfig
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Base URL from BuildConfig — different per build type (debug/release)
    private const val BASE_URL = BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS  // BODY was too slow (logs entire JWT tokens)
        }

        // Auth Interceptor: attaches JWT token to every request if available
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = tokenManager.getToken()
            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(5, TimeUnit.SECONDS)   // Fast-fail on local connection
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideJobApiService(retrofit: Retrofit): JobApiService {
        return retrofit.create(JobApiService::class.java)
    }
}
