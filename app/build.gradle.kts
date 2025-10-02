plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	// kotlin("kapt") // Removed - no longer needed without Room
	id("org.jetbrains.kotlin.plugin.serialization")
	id("com.google.gms.google-services")
}

android {
	namespace = "dev.solora"
	compileSdk = 35

	defaultConfig {
		applicationId = "dev.solora"
		minSdk = 24
		targetSdk = 35
		versionCode = 1
		versionName = "1.0.0"
	}

	signingConfigs {
		create("release") {
			storeFile = file("keystore/solora-release-key.keystore")
			storePassword = "solora123"
			keyAlias = "solora"
			keyPassword = "solora123"
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			signingConfig = signingConfigs.getByName("release")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}

	buildFeatures {
		viewBinding = true
	}
}

dependencies {
	// Firebase BOM and services
	implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
	implementation("com.google.firebase:firebase-auth-ktx")
	implementation("com.google.firebase:firebase-firestore-ktx")
	implementation("com.google.firebase:firebase-analytics-ktx")
	// Use BOM-managed version (no explicit version) to ensure API compatibility
	implementation("com.google.firebase:firebase-functions-ktx")

	// SSO - Google Sign-In
	implementation("com.google.android.gms:play-services-auth:20.7.0")
	implementation("androidx.core:core-ktx:1.13.1")
	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
	implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
	// Provides Task.await() for Firebase Tasks used in Functions calls
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
	implementation("androidx.datastore:datastore-preferences:1.1.1")
	implementation("androidx.datastore:datastore-preferences-core:1.1.1")

	// Room database removed for Part 2 - using Firebase Firestore only

	// WorkManager for sync
	implementation("androidx.work:work-runtime-ktx:2.9.1")

	// Networking
	implementation("io.ktor:ktor-client-android:2.3.12")
	implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
	implementation("io.ktor:ktor-client-logging:2.3.12")
	implementation("io.ktor:ktor-client-core:2.3.12")
	implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

	// PDF
	implementation("com.itextpdf:itext7-core:7.2.5")

	// Accompanist permissions
	implementation("com.google.accompanist:accompanist-permissions:0.36.0")

	// Fragments and Navigation Component (for fragment-based navigation)
	implementation("androidx.fragment:fragment-ktx:1.8.4")
	implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
	implementation("androidx.navigation:navigation-ui-ktx:2.8.0")

	// Lifecycle
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
	implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

	// Debug
}


// Force Gradle sync Mon Sep 29 12:45:34 SAST 2025
