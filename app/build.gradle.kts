plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.attentioncoach"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.attentioncoach"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui-android:1.9.2")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.9.2")
    implementation("androidx.compose.foundation:foundation-android:1.9.2")
    implementation("androidx.compose.material3:material3-android:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    debugImplementation("androidx.compose.ui:ui-tooling-android:1.9.2")

    testImplementation("junit:junit:4.13.2")
}

