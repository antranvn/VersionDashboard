package org.example.project.bitbucket

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Minimal read-only client for the Bitbucket **Server / Data Center** REST API v1.0.
 *
 * Pass an HTTP access [token] (sent as `Authorization: Bearer …`) and set [baseUrl] to
 * the server root (e.g. "https://bitbucketp.id.dbsnet.com"); the "/rest/api/1.0/..." path
 * is appended here. [token] may be null for unauthenticated/anonymous endpoints.
 */
class BitbucketClient(
    token: String? = null,
    baseUrl: String = "http://localhost:8081",
    httpClient: HttpClient? = null,
) {
    private val baseUrl: String = baseUrl.trimEnd('/')

    private val client: HttpClient = (httpClient ?: HttpClient()).config {
        expectSuccess = true

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }

        if (token != null) {
            defaultRequest { header(HttpHeaders.Authorization, "Bearer $token") }
        }
    }

    /**
     * Lists all repositories in a project, following pagination to completion.
     * @param projectKey the project key (e.g. "CORE").
     * @param pageSize results requested per page (Bitbucket Server caps this server-side).
     */
    suspend fun listRepositories(projectKey: String, pageSize: Int = 100): List<Repository> {
        val all = mutableListOf<Repository>()
        var start = 0
        while (true) {
            val page = client.get("$baseUrl/rest/api/1.0/projects/$projectKey/repos") {
                parameter("limit", pageSize)
                parameter("start", start)
            }.body<Paginated<Repository>>()
            all += page.values
            val next = page.nextPageStart
            if (page.isLastPage || next == null) break
            start = next
        }
        return all
    }

    /** Fetches a single repository by its slug within a project. */
    suspend fun getRepository(projectKey: String, repoSlug: String): Repository =
        client.get("$baseUrl/rest/api/1.0/projects/$projectKey/repos/$repoSlug").body()

    /** Releases the underlying HTTP engine. Call when the client is no longer needed. */
    fun close() = client.close()
}
