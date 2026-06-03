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

/** The local dev proxy from the `:proxy` module. Start it with `./gradlew :proxy:run`. */
private const val PROXY_BASE_URL = "http://localhost:8081"

@Composable
fun RepoListScreen() {
    val scope = rememberCoroutineScope()
    // No credentials here — the proxy injects them server-side.
    val client = remember { BitbucketClient(baseUrl = PROXY_BASE_URL) }

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
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            repos = client.listRepositories(projectKey.trim())
                        } catch (e: Throwable) {
                            error = e.message ?: e.toString()
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
