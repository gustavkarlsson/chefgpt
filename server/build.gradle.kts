plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.slapshot)
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
    implementation(libs.ktorServerSessions)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)

    // Misc
    implementation(libs.kotlinResult)

    // Koog
    implementation(libs.koogKtor)

    // Database (R2DBC for application, JDBC for Flyway migrations)
    implementation(libs.exposedCore)
    implementation(libs.exposedR2dbc)
    implementation(libs.exposedJson)
    implementation(libs.r2dbcPostgres)
    implementation(libs.r2dbcPool)
    implementation(libs.postgresDriver)
    implementation(libs.flyway)
    implementation(libs.flywayPostgres)

    // Test
    testImplementation(libs.ktorServerTestHost)
    testImplementation(libs.kotlinTestJunit5)
    testImplementation(libs.slapshotJunit5)
    testImplementation(libs.slapshotKtor3)
    testImplementation(libs.testcontainersJunit5)
    testImplementation(libs.testcontainersPostgres)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("postgres") {
    group = "application"
    description = "Ensures the chefgpt postgres docker container is running"

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

        fun runOrThrow(vararg cmd: String) {
            val (exitCode, output) = run(*cmd)
            if (exitCode != 0) {
                throw GradleException("Command failed (exit $exitCode): ${cmd.joinToString(" ")}\n$output")
            }
        }

        val (exitCode, output) = run("docker", "inspect", "-f", "{{.State.Running}}", "chefgpt")
        when {
            exitCode != 0 -> {
                logger.lifecycle("Creating and starting chefgpt container...")
                runOrThrow(
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
                runOrThrow("docker", "start", "chefgpt")
            }

            else -> {
                logger.lifecycle("chefgpt container is already running.")
            }
        }
    }
}

tasks.named("run") {
    mustRunAfter("postgres")
}
