import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
}

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )

    // TODO fix deprecation
    @Suppress("DEPRECATION")
    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ChefGPT"
            isStatic = true
        }
    }

    jvm {
        mainRun {
            mainClass = "se.gustavkarlsson.chefgpt.MainKt"
        }
    }

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation(libs.composeMaterial3)
            implementation(libs.composeUi)
            implementation(libs.composeComponentsResources)
            implementation(libs.composeUiToolingPreview)
            implementation(libs.androidxLifecycleViewmodelCompose)
            implementation(libs.androidxLifecycleRuntimeCompose)
            // TODO specify dependency instead of compose.materialIconsExtended
            implementation(compose.materialIconsExtended)
            implementation(libs.multiplatformMarkdownRendererM3)
            implementation(libs.ktorClientCore)
            implementation(libs.ktorClientCio)
            implementation(libs.ktorClientContentNegotiation)
            implementation(libs.ktorSerializationKotlinxJson)
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.coilCompose)
            implementation(libs.coilNetworkKtor)
        }
        commonTest.dependencies {
            implementation(libs.kotlinTest)
        }
        androidMain.dependencies {
            implementation(libs.composeUiToolingPreview) // TODO Move to common?
            implementation(libs.androidxActivityCompose)
        }
        jvmMain.dependencies {
            implementation(libs.slf4jApi)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinxCoroutinesSwing)
        }
    }
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
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    debugImplementation(libs.composeUiTooling)
}

compose.desktop {
    application {
        mainClass = "se.gustavkarlsson.chefgpt.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "se.gustavkarlsson.chefgpt"
            packageVersion = "1.0.0"
        }
    }
}
