plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
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

    buildFeatures { compose = true }
}

dependencies {
	implementation(platform("androidx.compose:compose-bom:2024.09.02"))
	implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.activity:activity-compose:1.9.2")
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-tooling-preview")
        implementation("androidx.compose.material3:material3:1.3.0")
    implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
        implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.3")
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

	// Debug
	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")
}


