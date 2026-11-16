plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ink.blavingad.hcb"
    compileSdk = 37

    defaultConfig {
        applicationId = "ink.blavingad.hcb"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "0.1.0"
        buildConfigField("String", "API_BASE", "\"https://hcb.hackclub.com/api/v4\"")
        buildConfigField("String", "OAUTH_CLIENT_ID", "\"yt8JHmPDmmYYLUmoEiGtocYwg5fSOGCrcIY3G-vkMRs\"")
        buildConfigField("String", "OAUTH_REDIRECT_URI", "\"hcb://\"")
        buildConfigField("String", "STRIPE_PK", "\"pk_live_UAjIP1Kss29XZ6tW0MFWkjUQ\"")
    }

    // Release signing from env (CI); falls back to the debug key so a local release build still installs.
    val ksFile = System.getenv("HCB_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (ksFile != null) {
            create("release") {
                storeFile = ksFile
                storePassword = System.getenv("HCB_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HCB_KEY_ALIAS")
                keyPassword = System.getenv("HCB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.browser)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.work.runtime)
    implementation(libs.biometric)
    implementation(libs.fragment)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
}
