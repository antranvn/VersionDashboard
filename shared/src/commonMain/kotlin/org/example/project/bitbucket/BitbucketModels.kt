package org.example.project.bitbucket

import kotlinx.serialization.Serializable

/**
 * A subset of the fields returned by the Bitbucket **Server / Data Center**
 * REST API v1.0 (https://{host}/rest/api/1.0). Unknown fields are ignored
 * (see [BitbucketClient]'s JSON config), so only the needed properties are declared.
 */

/** One page of a paginated Bitbucket Server response. */
@Serializable
data class Paginated<T>(
    val values: List<T> = emptyList(),
    val size: Int = 0,
    val limit: Int = 0,
    val start: Int = 0,
    val isLastPage: Boolean = true,
    /** Start index of the next page; null/absent on the last page. */
    val nextPageStart: Int? = null,
)

@Serializable
data class Repository(
    val id: Int,
    val slug: String,
    val name: String,
    val description: String? = null,
    /** e.g. "AVAILABLE". */
    val state: String? = null,
    val public: Boolean = false,
    val project: Project? = null,
)

@Serializable
data class Project(
    val key: String,
    val name: String? = null,
)
