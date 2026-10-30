plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.snow34.calendarwidget"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.snow34.calendarwidget"
        // 30+: BiometricPrompt supports BIOMETRIC | DEVICE_CREDENTIAL authenticators.
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.work.runtime)
    implementation(libs.okhttp)
    implementation(libs.biometric)
    implementation(libs.fragment)
}

// Privacy guarantee: fail the build if anything Google-services-shaped ends up in the runtime classpath.
val bannedGroups = listOf(
    "com.google.android.gms",
    "com.google.firebase",
    "com.google.android.play",
    "com.google.mlkit",
    "com.google.android.datatransport",
)

tasks.register("verifyNoGoogleServices") {
    group = "verification"
    description = "Fails if Play Services / Firebase / Play Core are on any runtime classpath."
    notCompatibleWithConfigurationCache("inspects resolved configurations at execution time")
    doLast {
        val offenders = configurations
            .filter { it.isCanBeResolved && it.name.endsWith("RuntimeClasspath") }
            .flatMap { cfg -> cfg.incoming.resolutionResult.allComponents.mapNotNull { it.moduleVersion } }
            .filter { m -> bannedGroups.any { m.group.startsWith(it) } }
            .map { "${it.group}:${it.name}:${it.version}" }
            .toSortedSet()
        check(offenders.isEmpty()) { "Google services dependencies found: $offenders" }
    }
}
tasks.matching { it.name == "check" }.configureEach { dependsOn("verifyNoGoogleServices") }
