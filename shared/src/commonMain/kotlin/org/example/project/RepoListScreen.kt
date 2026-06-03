package org.example.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.bitbucket.BitbucketClient
import org.example.project.bitbucket.Repository

@Composable
fun RepoListScreen(client: BitbucketClient) {
    val scope = rememberCoroutineScope()

    var projectKey by remember { mutableStateOf("") }
    var repos by remember { mutableStateOf<List<Repository>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Bitbucket repositories", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = projectKey,
                onValueChange = { projectKey = it },
                label = { Text("Project key (e.g. CORE)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Button(
                enabled = projectKey.isNotBlank() && !loading,
                onClick = {
                    val key = projectKey.trim()
                    scope.launch {
                        loading = true
                        error = null
                        println("[RepoList] Fetching repos for project '$key'")
                        try {
                            val result = client.listRepositories(key)
                            println("[RepoList] Loaded ${result.size} repos for project '$key'")
                            repos = result
                        } catch (e: Throwable) {
                            println("[RepoList] Fetch failed for project '$key': ${e::class.simpleName}: ${e.message}")
                            error = friendlyError(e)
                            repos = emptyList()
                        } finally {
                            loading = false
                        }
                    }
                },
            ) { Text("Fetch") }
        }

        Spacer(Modifier.height(16.dp))

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(
                "Error: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            repos.isEmpty() -> Text(
                "Enter a project key and press Fetch.",
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(repos) { repo -> RepoRow(repo) }
            }
        }
    }
}

/**
 * Turns a raw exception into something actionable, pointing at the usual culprits
 * when the Bitbucket host can't be reached.
 */
private fun friendlyError(e: Throwable): String {
    val raw = e.message ?: e.toString()
    val looksLikeNetwork = listOf(
        "UnknownHost", "UnresolvedAddress", "Connection refused", "ConnectException",
        "timeout", "timed out", "SSLHandshake", "No route to host",
    ).any { raw.contains(it, ignoreCase = true) }
    return if (looksLikeNetwork) {
        buildString {
            appendLine("Couldn't reach the Bitbucket server.")
            appendLine("• Are you on the VPN that can reach the host?")
            appendLine("• Is bitbucket.baseUrl correct in local.properties?")
            appendLine("• Is bitbucket.token set and valid?")
            append("(details: $raw)")
        }
    } else {
        "$raw\n(see the run terminal for the full log)"
    }
}

@Composable
private fun RepoRow(repo: Repository) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(repo.name, style = MaterialTheme.typography.titleMedium)
            Text(
                listOfNotNull(repo.project?.key, repo.slug).joinToString("/"),
                style = MaterialTheme.typography.bodySmall,
            )
            repo.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(if (repo.public) "public" else "private")
                    repo.state?.takeIf { it.isNotBlank() }?.let { append(" • ").append(it) }
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
