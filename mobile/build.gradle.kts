import org.gradle.api.tasks.compile.JavaCompile

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_RELEASE_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("ANDROID_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(releaseKeystorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
	.all { !it.isNullOrBlank() }

android {
	namespace = "com.ziacik.cookcue.mobile"
	compileSdk = 37

	defaultConfig {
		applicationId = "com.ziacik.cookcue"
		minSdk = 26
		targetSdk = 37
		versionCode = providers.gradleProperty("releaseVersionCode").orNull?.toInt() ?: 10
		versionName = providers.gradleProperty("releaseVersionName").orNull ?: "0.10.0"
	}

	if (hasReleaseSigning) {
		signingConfigs {
			create("release") {
				storeFile = file(requireNotNull(releaseKeystorePath))
				storePassword = requireNotNull(releaseStorePassword)
				keyAlias = requireNotNull(releaseKeyAlias)
				keyPassword = requireNotNull(releaseKeyPassword)
			}
		}
		buildTypes {
			getByName("release") {
				signingConfig = signingConfigs.getByName("release")
			}
		}
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
