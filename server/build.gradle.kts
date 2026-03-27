import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
    application
}

group = "se.gustavkarlsson.chefgpt"
version = "1.0.0"

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

application {
    mainClass.set("se.gustavkarlsson.chefgpt.ApplicationKt")

    val development =
        project.ext
            .get("development")
            ?.toString()
            ?.toBooleanStrictOrNull()
    if (development != null) {
        applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$development")
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.logback)
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerDi)
    implementation(libs.ktorServerSse)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)

    // Koog
    implementation(libs.koogKtor)

    // Database
    implementation(libs.hikari)
    implementation(libs.postgresDriver)
    implementation(libs.exposedCore)
    implementation(libs.exposedJdbc)
    implementation(libs.exposedDao)
    implementation(libs.flyway)
    implementation(libs.flywayPostgres)

    // Test
    testImplementation(libs.ktorServerTestHost)
    testImplementation(libs.kotlinTestJunit)
}

tasks.register("runWithDocker") {
    group = "application"
    description = "Ensures the chefgpt postgres container is running, then runs the server"

    doFirst {
        fun run(vararg cmd: String): Pair<Int, String> {
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val out =
                proc.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            return proc.waitFor() to out
        }

        val (exitCode, output) = run("docker", "inspect", "-f", "{{.State.Running}}", "chefgpt")
        when {
            exitCode != 0 -> {
                logger.lifecycle("Creating and starting chefgpt container...")
                run(
                    "docker",
                    "run",
                    "--name",
                    "chefgpt",
                    "--detach",
                    "--publish",
                    "127.0.0.1:5432:5432",
                    "--env",
                    "POSTGRES_PASSWORD=password",
                    "--env",
                    "POSTGRES_DB=chefgpt",
                    "postgres:18.3",
                )
            }

            output != "true" -> {
                logger.lifecycle("Starting existing chefgpt container...")
                run("docker", "start", "chefgpt")
            }

            else -> {
                logger.lifecycle("chefgpt container is already running.")
            }
        }
    }

    finalizedBy("run")
}
