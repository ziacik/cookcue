import org.gradle.api.tasks.compile.JavaCompile

plugins {
	id("com.android.library")
}

android {
	namespace = "com.ziacik.cookcue.core"
	compileSdk = 37

	defaultConfig {
		minSdk = 26
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
	testImplementation("junit:junit:4.13.2")
}
