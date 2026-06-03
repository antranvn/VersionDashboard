package org.example.project.proxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.queryString
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File
import java.util.Base64
import java.util.Properties

/**
 * Minimal local development proxy for the Bitbucket Cloud REST API v2.
 *
 * Why this exists:
 *  - The browser can't call api.bitbucket.org directly (CORS), and we don't want
 *    the Bitbucket credential shipped in the public web bundle.
 *  - This server holds the credential server-side, injects HTTP Basic auth, adds
 *    permissive CORS headers (DEV ONLY), and forwards any `/2.0/...` path through
 *    to Bitbucket unchanged. The web app calls this instead of Bitbucket.
 *
 * Credentials are read from (in order):
 *  1. BITBUCKET_USERNAME / BITBUCKET_APP_PASSWORD environment variables
 *  2. bitbucket.username / bitbucket.appPassword in local.properties (repo root)
 *
 * Run: ./gradlew :proxy:run
 */
private const val PORT = 8081

fun main() {
    val (user, pass) = loadCredentials()

    // Set the Basic auth header directly rather than via the client Auth plugin: a
    // forwarding proxy should pass error responses (e.g. 401) straight through, not
    // try to parse Bitbucket's WWW-Authenticate challenge and retry.
    val authHeader = "Basic " + Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
    val forwarder = HttpClient(ClientCIO) {
        expectSuccess = false
    }

    println("Bitbucket proxy listening on http://localhost:$PORT (forwarding as '$user')")

    embeddedServer(ServerCIO, port = PORT) {
        install(CORS) {
            anyHost() // DEV ONLY — fine for localhost, never ship this as-is.
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
        }
        routing {
            // Transparently forward any /2.0/* path to Bitbucket, preserving the query string.
            get("/2.0/{path...}") {
                val tail = call.parameters.getAll("path").orEmpty().joinToString("/")
                val query = call.request.queryString()
                val target = buildString {
                    append("https://api.bitbucket.org/2.0/")
                    append(tail)
                    if (query.isNotEmpty()) append('?').append(query)
                }
                try {
                    val resp: HttpResponse = forwarder.get(target) {
                        header(HttpHeaders.Authorization, authHeader)
                    }
                    call.respondText(resp.bodyAsText(), ContentType.Application.Json, resp.status)
                } catch (e: Throwable) {
                    call.respondText(
                        """{"error":"proxy_forward_failed","message":${jsonString(e.message ?: e.toString())}}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadGateway,
                    )
                }
            }
        }
    }.start(wait = true)
}

/** Minimal JSON string escaping for the error payload. */
private fun jsonString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

private fun loadCredentials(): Pair<String, String> {
    System.getenv("BITBUCKET_USERNAME")?.let { u ->
        System.getenv("BITBUCKET_APP_PASSWORD")?.let { p -> return u to p }
    }
    for (path in listOf("local.properties", "../local.properties")) {
        val file = File(path)
        if (!file.exists()) continue
        val props = Properties().apply { file.inputStream().use { load(it) } }
        val user = props.getProperty("bitbucket.username")
        val pass = props.getProperty("bitbucket.appPassword")
        if (!user.isNullOrBlank() && !pass.isNullOrBlank()) return user to pass
    }
    error(
        "No Bitbucket credentials found. Set BITBUCKET_USERNAME and BITBUCKET_APP_PASSWORD " +
            "environment variables, or add bitbucket.username and bitbucket.appPassword to local.properties.",
    )
}
