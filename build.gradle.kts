// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Use ONLY aliases from libs.versions.toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}