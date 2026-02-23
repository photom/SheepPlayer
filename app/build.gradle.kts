plugins {
    alias(libs.plugins.android.application)
}

android {
    signingConfigs {
        create("release") {
            // Security: Use proper release signing configuration
            // TODO: Replace with production keystore for release builds
            // storeFile = file("path/to/release.keystore")
            // keyAlias = "release_key"
            // keyPassword = System.getenv("KEYSTORE_PASSWORD")
            // storePassword = System.getenv("STORE_PASSWORD")

            // WARNING: Debug keystore should only be used for development
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            keyAlias = "AndroidDebugKey"
            keyPassword = "android"
            storePassword = "android"
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
            signingConfig = signingConfigs.getByName("release")
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
