# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

VersionDashboard — a Kotlin Multiplatform / Compose Multiplatform app targeting the Web (Kotlin/Wasm and Kotlin/JS).

- `shared/` — shared code and Compose UI (`commonMain`), with `wasmJsMain` / `jsMain` platform sources.
- `webApp/` — runnable web app; `main.kt` mounts `App()` into a `ComposeViewport`.
- `gradle/libs.versions.toml` — version catalog for all dependencies.

### Run
- Wasm: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- JS: `./gradlew :webApp:jsBrowserDevelopmentRun`

### Test
- `./gradlew :shared:wasmJsTest` / `./gradlew :shared:jsTest`

## Rules

- **Never `git commit` or `git push` without a direct, explicit order from the user.** Make and stage changes freely, but committing and pushing require the user to explicitly ask each time. Approval for one commit/push does not carry over to the next.
