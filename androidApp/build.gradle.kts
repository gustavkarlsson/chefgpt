plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "se.gustavkarlsson.chefgpt"
    compileSdk =
        libs.versions.androidCompileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "se.gustavkarlsson.chefgpt"
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.androidTargetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        getByName("debug") {
            // Checked in so every machine and CI sign debug builds identically.
            storeFile = file("debug.keystore")
            storeType = "PKCS12"
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            // Allow plain-HTTP traffic so debug builds can reach the local dev server.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        getByName("release") {
            isMinifyEnabled = false
            // Release builds must talk to the server over HTTPS only.
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }
}

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

dependencies {
    implementation(projects.app)
    implementation(libs.androidxActivityCompose)
    implementation(libs.composeRuntime)
    implementation(libs.composeUiToolingPreview)
}
