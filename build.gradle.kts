import com.diffplug.gradle.spotless.BaseKotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.spotless)
}

tasks.register("buildSupported") {
    group = "build"
    description = "Builds all supported targets (server, JVM, web WASM, web JS, and conditionally Android and iOS)"

    // Always-available targets
    dependsOn(
        ":server:shadowJar",
        ":app:jvmJar",
        ":app:wasmJsBrowserDistribution",
        ":app:jsBrowserDistribution",
    )

    // Android: only if Android build tools are available
    val hasAndroid =
        try {
            val result =
                providers
                    .exec {
                        commandLine("which", "adb")
                        isIgnoreExitValue = true
                    }.result
                    .get()
            result.exitValue == 0
        } catch (_: Exception) {
            false
        }

    if (hasAndroid) {
        dependsOn(":app:assembleDebug")
    } else {
        doFirst {
            logger.warn("WARNING: Android tools (adb) not found — skipping Android build")
        }
    }

    // iOS: only if Xcode tools are available
    val hasXcode =
        try {
            val result =
                providers
                    .exec {
                        commandLine("which", "xcodebuild")
                        isIgnoreExitValue = true
                    }.result
                    .get()
            result.exitValue == 0
        } catch (_: Exception) {
            false
        }

    if (hasXcode) {
        dependsOn(":app:linkDebugFrameworkIosSimulatorArm64")
    } else {
        doFirst {
            logger.warn("WARNING: Xcode tools (xcodebuild) not found — skipping iOS build")
        }
    }
}

allprojects {
    apply(
        plugin =
            rootProject.libs.plugins.spotless
                .get()
                .pluginId,
    )

    tasks.withType<KotlinCompilationTask<*>> {
        compilerOptions {
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    spotless {
        val commonKotlinConfig: BaseKotlinExtension.() -> Unit = {
            ktlint(libs.versions.ktlint.get())
        }
        kotlin {
            target("**/*.kt")
            targetExclude("**/generated/**") // build directory is excluded automatically
            commonKotlinConfig()
        }
        // Separate config for scripts such as build.gradle.kts files
        kotlinGradle {
            target("**/*.kts")
            targetExclude("**/generated/**") // build directory is excluded automatically
            commonKotlinConfig()
        }
    }
}
