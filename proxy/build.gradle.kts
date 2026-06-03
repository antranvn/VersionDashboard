plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.auth)
}

application {
    mainClass.set("org.example.project.proxy.ProxyServerKt")
}

// Run from the repo root so the server can read credentials from local.properties.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    standardInput = System.`in`
}
