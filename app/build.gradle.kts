plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
    kotlin("kapt")
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

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
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
	implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
        implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

	// Room
	implementation("androidx.room:room-runtime:2.6.1")
	kapt("androidx.room:room-compiler:2.6.1")
	implementation("androidx.room:room-ktx:2.6.1")

	// WorkManager for sync
	implementation("androidx.work:work-runtime-ktx:2.9.1")

	// Networking
	implementation("io.ktor:ktor-client-android:2.3.12")
	implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
	implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

	// PDF
	implementation("com.itextpdf:itext7-core:7.2.5")

	// Accompanist permissions
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Fragments and Navigation Component (for fragment-based navigation)
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.0")

    // Debug
}


