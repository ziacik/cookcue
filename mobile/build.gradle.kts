import org.gradle.api.tasks.compile.JavaCompile

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
		versionCode = 6
		versionName = "0.6.0"
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}

	lint {
		warningsAsErrors = true
		abortOnError = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}


kotlin {
	compilerOptions {
		allWarningsAsErrors.set(true)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
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
	implementation("io.coil-kt:coil-compose:2.7.0")

	debugImplementation("androidx.compose.ui:ui-tooling")
}
