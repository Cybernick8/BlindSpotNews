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

    buildFeatures {
        compose = true
        viewBinding = true

    }
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
}

dependencies {

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-functions-ktx")

    // implementation(libs.firebase.storage.ktx)
    // implementation("com.google.firebase:firebase-storage-ktx")

    // Auth / identity
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Gson / networking
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jsoup:jsoup:1.17.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Core constraints (keep)
    constraints {
        implementation("androidx.core:core:1.13.1")
        implementation("androidx.core:core-ktx:1.13.1")
    }

    // AndroidX base
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose
    implementation(platform(libs.androidx.compose.bom.v20241001))
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.material3.android)
    implementation(libs.material3)
    implementation(libs.androidx.foundation)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.ui.tooling)

    // Coil
    implementation(libs.coil.compose)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
