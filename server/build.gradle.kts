plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "se.gustavkarlsson.chefgpt"
version = "1.0.0"
application {
    mainClass.set("se.gustavkarlsson.chefgpt.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.logback)
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerWebsockets)
    implementation(libs.ktorServerContentNegotiation) // TODO do we need 'em?
    implementation(libs.ktorServerDi) // TODO do we need 'em?
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.koogKtor)
    testImplementation(libs.ktorServerTestHost)
    testImplementation(libs.kotlinTestJunit)
}
