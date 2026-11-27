plugins {
    alias(libs.plugins.android.application)
}

/**
 * Reads a release-signing value from a Gradle property (e.g. in the user's
 * `~/.gradle/gradle.properties`) or, failing that, an environment variable.
 * Signing credentials are never stored in the repository.
 */
fun releaseSigningValue(property: String, environmentVariable: String): String? =
    providers.gradleProperty(property)
        .orElse(providers.environmentVariable(environmentVariable))
        .orNull

val releaseStoreFile = releaseSigningValue("sheepplayer.release.storeFile", "SHEEPPLAYER_RELEASE_STORE_FILE")

android {
    signingConfigs {
        // Release builds are signed only when a production keystore is provided.
        // Otherwise the release APK is left unsigned rather than signed with the
        // debug key, so CI and local `./gradlew build` work without secrets.
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseSigningValue("sheepplayer.release.storePassword", "SHEEPPLAYER_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigningValue("sheepplayer.release.keyAlias", "SHEEPPLAYER_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningValue("sheepplayer.release.keyPassword", "SHEEPPLAYER_RELEASE_KEY_PASSWORD")
            }
        }
    }
    namespace = "com.hitsuji.sheepplayer2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hitsuji.sheepplayer2"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/INDEX.LIST"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.github.bumptech.glide:glide:5.0.5")

    // Google Drive API and Authentication
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20251210-2.0.0")
    implementation("com.google.api-client:google-api-client-android:2.8.1")
    implementation("com.google.api-client:google-api-client-gson:2.8.1")
    implementation("com.google.http-client:google-http-client-gson:2.1.0")
    implementation("com.google.http-client:google-http-client:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    
    // Audio metadata extraction
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
