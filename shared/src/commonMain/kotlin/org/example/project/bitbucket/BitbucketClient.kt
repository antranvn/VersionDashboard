package org.example.project.bitbucket

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Minimal read-only client for the Bitbucket Cloud REST API v2.
 *
 * Two usage modes:
 *  - **Via proxy (recommended for the web app):** leave [username]/[appPassword] null
 *    and point [baseUrl] at the local proxy (e.g. "http://localhost:8081"). The proxy
 *    injects auth and handles CORS, so no credential touches the browser.
 *  - **Direct (e.g. from a JVM/test, no browser CORS):** pass [username] + an
 *    app password and leave [baseUrl] at its default.
 */
class BitbucketClient(
    username: String? = null,
    appPassword: String? = null,
    baseUrl: String = "https://api.bitbucket.org",
    httpClient: HttpClient? = null,
) {
    private val baseUrl: String = baseUrl.trimEnd('/')

    private val client: HttpClient = run {
        // Captured as differently-named locals so the credentials lambda doesn't bind
        // to the auth config's (deprecated) receiver properties of the same name.
        val user = username
        val pass = appPassword
        (httpClient ?: HttpClient()).config {
            expectSuccess = true

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }

            if (user != null && pass != null) {
                install(Auth) {
                    basic {
                        credentials { BasicAuthCredentials(username = user, password = pass) }
                        sendWithoutRequest { true }
                    }
                }
            }
        }
    }

    /**
     * Lists repositories in a workspace.
     * @param workspace the workspace slug (e.g. "antranvn").
     * @param pageLen number of results per page (Bitbucket max is 100).
     */
    suspend fun listRepositories(workspace: String, pageLen: Int = 50): List<Repository> =
        client.get("$baseUrl/2.0/repositories/$workspace") {
            parameter("pagelen", pageLen)
        }.body<Paginated<Repository>>().values

    /** Fetches a single repository by its slug within a workspace. */
    suspend fun getRepository(workspace: String, repoSlug: String): Repository =
        client.get("$baseUrl/2.0/repositories/$workspace/$repoSlug").body()

    /** Releases the underlying HTTP engine. Call when the client is no longer needed. */
    fun close() = client.close()
}
