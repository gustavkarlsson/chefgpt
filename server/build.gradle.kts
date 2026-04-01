import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.slapshot)
    alias(libs.plugins.sqldelight)
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
    implementation(libs.kotlinxCoroutinesReactive)
    implementation(libs.logback)
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.koinKtor)
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

    // Database (R2DBC + SQLDelight)
    implementation(libs.sqldelightR2dbcDriver)
    implementation(libs.sqldelightCoroutines)
    implementation(libs.sqldelightRuntime)
    implementation(libs.r2dbcPool)
    implementation(libs.r2dbcPostgres)
    implementation(libs.postgresDriver)
    implementation(libs.flyway)
    implementation(libs.flywayPostgres)

    // RethinkDB
    implementation(libs.rethinkdb)

    // Test
    testImplementation(libs.ktorServerTestHost)
    testImplementation(libs.kotlinTestJunit5)
    testImplementation(libs.slapshotJunit5)
    testImplementation(libs.slapshotKtor3)
    // TODO Remove once testcontainer example is replaced with proper integration tests
    testImplementation(libs.postgresDriver)
    testImplementation(libs.testcontainersJunit5)
    testImplementation(libs.testcontainersPostgres)
}

val flywayMigrationDirectory = layout.buildDirectory.dir("resources/main/db/migration")
val sqldelightMigrationTempDirectory = layout.buildDirectory.dir("tmp/sqldelight/migrations")

sqldelight {
    databases {
        create("ChefGptDatabase") {
            packageName.set("se.gustavkarlsson.chefgpt.db")
            dialect(libs.sqldelightPostgresDialect)
            verifyMigrations.set(true)
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            generateAsync.set(true)
            deriveSchemaFromMigrations.set(true)
            migrationOutputDirectory.set(sqldelightMigrationTempDirectory)
        }
    }
}

val copySqldelightMigrationsToFlywheel =
    tasks.register<Copy>("copySqldelightMigrationsToFlywheel") {
        description =
            "Copies SQLDelight migrations from a temporary to the Flyway migrations directory, renaming them to match Flyway's format"
        dependsOn("generateMainChefGptDatabaseMigrations")
        from(sqldelightMigrationTempDirectory) {
            include("*")
        }
        into(flywayMigrationDirectory)
        rename {
            val version = it.takeWhile { it.isDigit() }.toInt()
            val newName = "V${version}__migration.sql"
            logger.info("Renaming migration file from $it to $newName")
            newName
        }
    }

tasks.withType<KotlinCompile> {
    dependsOn(copySqldelightMigrationsToFlywheel)
}

tasks.test {
    useJUnitPlatform()
}

object Command {
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
}

val postgres =
    tasks.register("postgres") {
        group = "application"
        description = "Ensures the chefgpt postgres docker container is running"

        doFirst {
            val containerName = "chefgpt-postgres"
            val (exitCode, output) = Command.run("docker", "inspect", "-f", "{{.State.Running}}", containerName)
            when {
                exitCode != 0 -> {
                    logger.lifecycle("Creating and starting $containerName container...")
                    Command.runOrThrow(
                        "docker",
                        "run",
                        "--name",
                        containerName,
                        "--detach",
                        "--publish",
                        "127.0.0.1:5432:5432",
                        "--env",
                        "POSTGRES_HOST_AUTH_METHOD=trust",
                        "--env",
                        "POSTGRES_DB=chefgpt",
                        "postgres:18.3",
                    )
                }

                output != "true" -> {
                    logger.lifecycle("Starting existing $containerName container...")
                    Command.runOrThrow("docker", "start", containerName)
                }

                else -> {
                    logger.lifecycle("$containerName container is already running.")
                }
            }
        }
    }

val rethinkdb =
    tasks.register("rethinkdb") {
        group = "application"
        description = "Ensures the rethinkdb docker container is running"

        doFirst {
            val containerName = "chefgpt-rethinkdb"
            val (exitCode, output) = Command.run("docker", "inspect", "-f", "{{.State.Running}}", containerName)
            when {
                exitCode != 0 -> {
                    logger.lifecycle("Creating and starting $containerName container...")
                    Command.runOrThrow(
                        "docker",
                        "run",
                        "--name",
                        containerName,
                        "--detach",
                        "--publish",
                        "127.0.0.1:28015:28015",
                        "--publish",
                        "127.0.0.1:8085:8080",
                        "rethinkdb:2.4",
                    )
                }

                output != "true" -> {
                    logger.lifecycle("Starting existing $containerName container...")
                    Command.runOrThrow("docker", "start", containerName)
                }

                else -> {
                    logger.lifecycle("$containerName container is already running.")
                }
            }
        }
    }

tasks.named("run") {
    mustRunAfter(postgres, rethinkdb)
}
