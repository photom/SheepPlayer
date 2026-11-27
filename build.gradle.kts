// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP's built-in Kotlin support uses the Kotlin Gradle Plugin on the build
    // classpath; declaring it here pins it to the catalog version.
    alias(libs.plugins.kotlin.android) apply false
}
