plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.blindspotnews"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.blindspotnews"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }
    //composeOptions {
        // Must match your Kotlin plugin 1.9.24
        //kotlinCompilerExtensionVersion = "1.5.15"
    //}

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
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    // Pin Core so it doesn't demand SDK 36 / AGP 8.9+
    constraints {
        implementation("androidx.core:core:1.13.1")
        implementation("androidx.core:core-ktx:1.13.1")
    }
    // Base AndroidX (keep if you use them)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // ----- Jetpack Compose -----
    // One BOM to rule them all (works with Kotlin 1.9.24 / compiler 1.5.15)
    implementation(platform(libs.androidx.compose.bom.v20241001))
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.material3.android)
    implementation(libs.lifecycle.viewmodel.ktx)
    //implementation(libs.androidx.navigation.compose.android)
    //implementation(libs.androidx.navigation.compose.jvmstubs)
    debugImplementation(libs.ui.tooling)
    implementation(libs.material3)
    implementation(libs.androidx.foundation)  // Box/Row/Column/etc.
    // Activity integration for Compose (compatible with SDK 35 / AGP 8.8.x)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx) // optional but keeps versions aligned
    // Navigation for Compose (stable)
    implementation(libs.androidx.navigation.compose)
    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

}
