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
import java.util.Properties

/**
 * Minimal local development proxy for a Bitbucket **Server / Data Center** instance.
 *
 * Why this exists:
 *  - The browser can't call the Bitbucket host directly (CORS), and we don't want
 *    the HTTP access token shipped in the public web bundle.
 *  - This server holds the token server-side, injects "Authorization: Bearer ...",
 *    adds permissive CORS headers (DEV ONLY), and forwards any `/rest/...` path
 *    through to the configured Bitbucket host. The web app calls this instead.
 *
 * Config is read from (env wins over local.properties):
 *  - BITBUCKET_BASE_URL / bitbucket.baseUrl  (default: https://bitbucketp.id.dbsnet.com)
 *  - BITBUCKET_TOKEN     / bitbucket.token   (required; an HTTP access token)
 *
 * Run: ./gradlew :proxy:run
 *
 * Note: the host must be reachable from this machine (corporate VPN/network). If it
 * uses an internal CA, the JVM running this proxy must trust that CA.
 */
private const val PORT = 8081
private const val DEFAULT_BASE_URL = "https://bitbucketp.id.dbsnet.com"

private data class ProxyConfig(val baseUrl: String, val token: String)

fun main() {
    val config = loadConfig()
    val forwarder = HttpClient(ClientCIO) { expectSuccess = false }

    println("Bitbucket proxy listening on http://localhost:$PORT -> ${config.baseUrl}")

    embeddedServer(ServerCIO, port = PORT) {
        install(CORS) {
            anyHost() // DEV ONLY — fine for localhost, never ship this as-is.
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
        }
        routing {
            // Transparently forward any /rest/* path to the Bitbucket host, preserving the query string.
            get("/rest/{path...}") {
                val tail = call.parameters.getAll("path").orEmpty().joinToString("/")
                val query = call.request.queryString()
                val target = buildString {
                    append(config.baseUrl).append("/rest/").append(tail)
                    if (query.isNotEmpty()) append('?').append(query)
                }
                println("--> GET /rest/$tail${if (query.isNotEmpty()) "?$query" else ""}  ->  $target")
                try {
                    val resp: HttpResponse = forwarder.get(target) {
                        header(HttpHeaders.Authorization, "Bearer ${config.token}")
                    }
                    println("<-- ${resp.status} from $target")
                    call.respondText(resp.bodyAsText(), ContentType.Application.Json, resp.status)
                } catch (e: Throwable) {
                    System.err.println("!!! forward to $target failed: ${e::class.simpleName}: ${e.message}")
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

private fun loadConfig(): ProxyConfig {
    val props = Properties()
    for (path in listOf("local.properties", "../local.properties")) {
        val file = File(path)
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
            break
        }
    }
    val baseUrl = (System.getenv("BITBUCKET_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: props.getProperty("bitbucket.baseUrl")?.takeIf { it.isNotBlank() }
        ?: DEFAULT_BASE_URL).trimEnd('/')
    val token = System.getenv("BITBUCKET_TOKEN")?.takeIf { it.isNotBlank() }
        ?: props.getProperty("bitbucket.token")?.takeIf { it.isNotBlank() }
        ?: error(
            "No Bitbucket token found. Set the BITBUCKET_TOKEN environment variable, " +
                "or add bitbucket.token to local.properties (an HTTP access token).",
        )
    return ProxyConfig(baseUrl, token)
}
