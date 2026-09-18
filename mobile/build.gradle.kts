plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.plugin.compose")
}

android {
	namespace = "com.ziacik.cookcue.mobile"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.ziacik.cookcue"
		minSdk = 26
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
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("com.google.android.gms:play-services-wearable:20.0.1")

	debugImplementation("androidx.compose.ui:ui-tooling")
}
