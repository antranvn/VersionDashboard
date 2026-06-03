package org.example.project.bitbucket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A subset of the fields returned by the Bitbucket Cloud REST API v2.
 * Unknown fields are ignored (see [BitbucketClient]'s JSON config), so only
 * the properties actually needed are declared here.
 */

/** One page of a paginated Bitbucket response. */
@Serializable
data class Paginated<T>(
    val values: List<T> = emptyList(),
    val page: Int? = null,
    val size: Int? = null,
    val pagelen: Int? = null,
    /** Absolute URL of the next page, or null when this is the last page. */
    val next: String? = null,
)

@Serializable
data class Repository(
    val uuid: String,
    val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("is_private") val isPrivate: Boolean = false,
    val description: String? = null,
    val language: String? = null,
    @SerialName("updated_on") val updatedOn: String? = null,
    @SerialName("created_on") val createdOn: String? = null,
    val mainbranch: Branch? = null,
)

@Serializable
data class Branch(
    val name: String,
)
