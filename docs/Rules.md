# Rules.md — Déngé Development Guidelines

## Tujuan

This document defines the **coding standards, workflows, and guardrails** for the Déngé project. All contributors — human or AI — must follow these rules. Consistency is non-negotiable.

---

## 1. Coding Conventions

### 1.1 General

| Convention           | Rule                                              |
|----------------------|---------------------------------------------------|
| Language             | Kotlin (100% — no Java source files)              |
| Max line length      | 120 characters                                     |
| Indentation          | 4 spaces (no tabs)                                 |
| Trailing commas      | Always use in multi-line parameter/argument lists  |
| Imports              | No wildcard imports (`*`); explicit only            |
| Nullability          | Prefer non-null types; use `?` only when genuinely nullable |
| String templates     | Use `"$variable"` / `"${expression}"` over concatenation |
| When expressions     | Prefer `when` over `if-else` chains (3+ branches)  |

### 1.2 Naming Conventions

| Element              | Convention           | Example                           |
|----------------------|----------------------|-----------------------------------|
| Package              | lowercase, dot-separated | `com.asla.denge.data`             |
| Class / Object       | PascalCase           | `PlayerManager`, `TrackCard`      |
| Function             | camelCase            | `fetchPlaylist()`, `onTrackClick()` |
| Property / Variable  | camelCase            | `currentTrack`, `isPlaying`       |
| Constant             | SCREAMING_SNAKE_CASE | `MAX_QUEUE_SIZE`, `API_BASE_URL`  |
| Composable function  | PascalCase           | `MiniPlayer()`, `SearchScreen()`  |
| ViewModel            | `{Screen}ViewModel`  | `HomeViewModel`, `SearchViewModel`|
| UseCase              | `{Action}{Entity}UseCase` | `SearchMusicUseCase`         |
| Repository           | `{Entity}Repository` | `MusicRepository`, `AuthRepository` |
| DAO                  | `{Entity}Dao`        | `TrackDao`, `PlaylistDao`         |
| Entity (Room)        | `{Name}Entity`       | `CachedTrackEntity`               |
| State (UI)           | `{Screen}UiState`    | `HomeUiState`, `SearchUiState`    |
| Event (UI)           | `{Screen}Event`      | `HomeEvent`, `SearchEvent`        |

### 1.3 File Organization

Each Kotlin file should follow this order:

1. Package declaration
2. Imports (sorted, no wildcards)
3. Top-level declarations (single public class/interface per file)
4. Extension functions (if tightly coupled to the class)
5. Private helper functions

### 1.4 Compose-Specific

| Rule                          | Detail                                                    |
|-------------------------------|-----------------------------------------------------------|
| Preview functions             | Suffix with `Preview`: `@Preview fun MiniPlayerPreview()` |
| State hoisting               | Always hoist state to the caller                           |
| Side effects                 | Use `LaunchedEffect`, `SideEffect` — never in composition  |
| Modifier parameter           | Always first optional parameter, default `Modifier`        |
| Remember                     | Use `remember` / `rememberSaveable` for expensive computations |
| Stable/Immutable annotations | Use `@Stable` / `@Immutable` on state classes              |

---

## 2. Style Guide (Kotlin)

Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

### 2.1 Formatting (enforced by `.editorconfig`)

```ini
[*.kt]
indent_size = 4
max_line_length = 120
insert_final_newline = true
trim_trailing_whitespace = true
```

### 2.2 Linting

| Tool       | Config File              | Purpose                     |
|------------|--------------------------|------------------------------|
| Ktlint     | `.editorconfig`          | Kotlin style enforcement     |
| Detekt     | `detekt.yml`             | Static analysis, code smells |

Run before every commit:

```bash
./gradlew ktlintCheck detektMain
```

---

## 3. Commit & PR Format

### 3.1 Conventional Commits

All commits MUST follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**Types:**

| Type       | When to use                                      |
|------------|--------------------------------------------------|
| `feat`     | New feature or functionality                      |
| `fix`      | Bug fix                                           |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `style`    | Formatting, missing semicolons, etc. (no logic change) |
| `docs`     | Documentation only changes                        |
| `test`     | Adding or correcting tests                        |
| `chore`    | Build process, CI, dependency updates             |
| `perf`     | Performance improvement                           |

**Scopes:** `player`, `auth`, `search`, `library`, `ui`, `data`, `db`, `nav`, `settings`, `build`

**Examples:**

```
feat(player): add background playback via foreground service
fix(auth): handle token refresh race condition
refactor(data): extract Innertube response mapping to dedicated mapper
docs(schema): add migration notes for v2 download feature
```

### 3.2 Commit Size

- One logical change per commit
- If a commit message needs "and" → split into two commits
- Max 72 characters for the subject line

---

## 4. Branch Strategy

```
main (protected)
  ├── feat/background-playback
  ├── feat/search-ui
  ├── fix/auth-token-refresh
  ├── refactor/innertube-client
  └── docs/update-schema
```

| Rule                         | Detail                                           |
|------------------------------|--------------------------------------------------|
| `main` branch                | Always deployable; protected from direct push     |
| Feature branches             | `feat/<short-description>`                        |
| Bug fix branches             | `fix/<short-description>`                         |
| Refactor branches            | `refactor/<short-description>`                    |
| Merge strategy               | Squash merge to `main` (clean history)            |
| Delete after merge           | Always delete feature branch after merge          |

---

## 5. AI / Contributor Restrictions

These rules apply to **all AI assistants** (Claude, ChatGPT, Antigravity, etc.) and human contributors:

### 5.1 Database Schema

- **DO NOT** alter any table in `Schema.md` without explicit discussion and approval
- **DO NOT** add/remove/rename columns without updating `Schema.md` first
- **DO NOT** write raw SQL — always use Room DAOs and type-safe queries

### 5.2 Secrets & Security

- **ALL** secrets (OAuth client ID, API keys) go in `local.properties` or `google-services.json`
- **NEVER** hardcode secrets in source code
- **NEVER** commit `local.properties`, `google-services.json`, or `*.keystore` files
- Auth tokens are stored **ONLY** in `EncryptedSharedPreferences` via `TokenStore`

### 5.3 Architecture

- **DO NOT** bypass the repository layer — ViewModels must never call API/DAO directly
- **DO NOT** add Android framework imports to `domain/` layer
- **DO NOT** create new modules without discussing the rationale
- **DO NOT** add any analytics, tracking, or telemetry code

### 5.4 Dependencies

- **DO NOT** add new Gradle dependencies without justification
- Prefer official Jetpack/Google libraries over third-party alternatives
- All dependencies must be **free and open-source**

---

## 6. Do / Don't List

### ✅ DO

| #  | Rule                                                                    |
|----|-------------------------------------------------------------------------|
| 1  | Use Compose for all new UI — no XML layouts                              |
| 2  | Use `StateFlow` for ViewModel → UI communication                         |
| 3  | Handle all errors gracefully — show user-friendly messages               |
| 4  | Write KDoc comments for all public functions and classes                  |
| 5  | Use `sealed class` / `sealed interface` for UI states and events         |
| 6  | Use Kotlin coroutines for all async operations                           |
| 7  | Test business logic (use cases) with unit tests                          |
| 8  | Use string resources for all user-facing text (no hardcoded strings)     |
| 9  | Follow Material 3 guidelines for component sizing and spacing            |
| 10 | Log errors with `Timber` (debug builds only)                             |

### ❌ DON'T

| #  | Rule                                                                    |
|----|-------------------------------------------------------------------------|
| 1  | Don't use `GlobalScope` — always use structured concurrency              |
| 2  | Don't block the main thread — all I/O on `Dispatchers.IO`               |
| 3  | Don't use `lateinit` for nullable types — use `by lazy` or null default  |
| 4  | Don't suppress lint warnings without a comment explaining why            |
| 5  | Don't use deprecated APIs — find the modern replacement                  |
| 6  | Don't store state in singletons — use ViewModel + DI                     |
| 7  | Don't mix concerns — keep UI, business logic, and data separate          |
| 8  | Don't use `Thread.sleep()` — use `delay()` in coroutines                 |
| 9  | Don't commit generated files (`/build/`, `.gradle/`, etc.)               |
| 10 | Don't add any user tracking, analytics, or data collection               |

---

## 7. Do-Not-Touch List

These files/directories should **never** be modified without explicit approval:

| Path                         | Reason                                                    |
|------------------------------|-----------------------------------------------------------|
| `docs/Schema.md`             | Schema changes require migration planning                  |
| `app/google-services.json`   | Auth config — should be set up once                        |
| `*.keystore`                 | Signing keys — never regenerate or modify                  |
| `gradle/wrapper/`            | Gradle wrapper — update only via `./gradlew wrapper`       |
| `.gitignore`                 | Carefully curated — additions need review                  |
| `data/remote/innertube/`     | Core API layer — changes can break all features            |
| `player/PlaybackService.kt`  | Foreground service — critical for background play          |
| `auth/TokenStore.kt`         | Security-critical — handles encrypted auth tokens          |

---

*Last updated: 2026-09-24*
