plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.atomicfu)
    application
}

group = "se.gustavkarlsson.chefgpt"
version = "1.0.0"
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
    implementation(libs.koogKtor)
    testImplementation(libs.ktorServerTestHost)
    testImplementation(libs.kotlinTestJunit)
}
