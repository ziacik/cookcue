plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.plugin.compose")
}

android {
	namespace = "com.ziacik.cookcue.wear"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.ziacik.cookcue.wear"
		minSdk = 30
		targetSdk = 37
		versionCode = 1
		versionName = "0.1.0"
	}

	buildFeatures {
		compose = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}

dependencies {
	implementation(project(":core"))

	val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
	implementation(composeBom)

	implementation("androidx.activity:activity-compose:1.13.0")
	implementation("androidx.compose.foundation:foundation")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.wear.compose:compose-material3:1.6.2")
}
