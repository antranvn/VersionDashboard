package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.example.project.bitbucket.BitbucketClient

@Composable
fun App(client: BitbucketClient) {
    MaterialTheme {
        RepoListScreen(client)
    }
}
