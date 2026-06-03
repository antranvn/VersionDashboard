package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.bitbucket.BitbucketClient
import java.io.File
import java.util.Properties

private const val DEFAULT_BASE_URL = "https://bitbucketp.id.dbsnet.com"

/**
 * Desktop entry point. Reads Bitbucket config from local.properties at runtime
 * (no proxy, no CORS — a JVM HTTP client talks to the server directly), builds the
 * client, and launches the Compose window.
 */
fun main() {
    val props = loadLocalProperties()
    val baseUrl = (props.getProperty("bitbucket.baseUrl")?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL)
        .trimEnd('/')
    val token = props.getProperty("bitbucket.token")?.takeIf { it.isNotBlank() }

    if (token == null) {
        System.err.println(
            "ERROR: bitbucket.token is missing/blank in local.properties. " +
                "Add an HTTP access token (Manage account -> HTTP access tokens) and re-run.",
        )
        return
    }

    println("Connecting to $baseUrl")
    val client = BitbucketClient(token = token, baseUrl = baseUrl)

    application {
        Window(onCloseRequest = ::exitApplication, title = "VersionDashboard") {
            App(client)
        }
    }
}

private fun loadLocalProperties(): Properties {
    val props = Properties()
    // The working directory may be the module dir or the repo root depending on how it's launched.
    for (path in listOf("local.properties", "../local.properties", "../../local.properties")) {
        val file = File(path)
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
            break
        }
    }
    return props
}
