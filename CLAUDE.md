# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

VersionDashboard — a Compose Multiplatform **Desktop (JVM)** app that lists repositories
from a Bitbucket Server / Data Center instance via its REST API v1.0.

- `shared/` — Compose UI (`RepoListScreen`, `App`) and the `BitbucketClient` (`commonMain`),
  with a `jvm` target.
- `desktopApp/` — desktop entry point (`Main.kt`); reads `bitbucket.baseUrl` / `bitbucket.token`
  from `local.properties` at runtime, builds the client, and launches the window.
- `gradle/libs.versions.toml` — version catalog for all dependencies.

Note: a JVM desktop client talks to Bitbucket directly — no proxy and no CORS (those were
only needed for the earlier browser target). Requires network/VPN access to the host.

### Run
- `./gradlew :desktopApp:run`  (set `bitbucket.token` in `local.properties` first)

### Test
- `./gradlew :shared:jvmTest`

## Rules

- **Never `git commit` or `git push` without a direct, explicit order from the user.** Make and stage changes freely, but committing and pushing require the user to explicitly ask each time. Approval for one commit/push does not carry over to the next.
