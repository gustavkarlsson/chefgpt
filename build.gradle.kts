import com.diffplug.gradle.spotless.BaseKotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

allprojects {
    apply(
        plugin =
            rootProject.libs.plugins.spotless
                .get()
                .pluginId,
    )

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
            freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
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
            commonKotlinConfig()
        }
    }
}
