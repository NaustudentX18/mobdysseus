plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mobdysseus.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mobdysseus.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.keystore")
            storePassword = System.getenv("MOBDYSSEUS_KEYSTORE_PASS") ?: "mobdysseus123"
            keyAlias = "mobdysseus"
            keyPassword = System.getenv("MOBDYSSEUS_KEYSTORE_PASS") ?: "mobdysseus123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        jniLibs {
            excludes += listOf(
                "**/libsdcpp.so",
                "**/libwhisper_jni.so",
                "**/libbark_jni.so",
                "**/libonnxruntime.so",
                "**/libonnxruntime4j_jni.so",
                "**/libmlkit_google_ocr_pipeline.so",
                "**/libmlkitcommonpipeline.so",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.llmedge)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
