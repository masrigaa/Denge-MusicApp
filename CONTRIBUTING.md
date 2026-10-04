# Contributing to Déngé

Thank you for your interest in contributing to Déngé! This document outlines the guidelines and workflow for
contributing to this project.

---

## 1. Prerequisites

Before building or contributing to Déngé, make sure you have:

- **Android Studio**: Android Studio Koala / Ladybug or newer.
- **JDK 17**: Configured as the Gradle JDK in Android Studio or via `JAVA_HOME`.
- **Android SDK**: API level 35 (Android 15) with Build-Tools installed.
- **Local Configuration**:
    Create a `local.properties` file in the root directory specifying your Android SDK path:
    ```properties
    sdk.dir=/path/to/your/Android/Sdk
    ```
    *(On Windows, escape backslashes: `sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk`)*

---

## 2. Building the Project

Ensure everything builds cleanly from the terminal before making changes:

```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows PowerShell / Command Prompt
.\gradlew.bat assembleDebug
```

---

## 3. Branching Strategy

All contributions branch off `main` and must follow these naming conventions:

- `feat/<short-description>`: New features or UI additions.
- `fix/<short-description>`: Bug fixes and issue patches.
- `docs/<short-description>`: Documentation changes or updates.
- `refactor/<short-description>`: Code restructuring with no behavioral change.

---

## 4. Coding Standards

- All contributors must read and strictly adhere to [docs/Rules.md](docs/Rules.md).
- **Language**: Kotlin 100% (no Java files).
- **Formatting**: 4-space indentation, no tabs, max 120 characters per line.
- **Imports**: Explicit imports only — wildcard imports (`*`) are prohibited.
- **Architecture**: MVI / MVVM pattern with clean separation of layers.

---

## 5. Commit Guidelines

We enforce **Conventional Commits** format:

```
<type>(<optional scope>): <description>
```

Allowed types:
- `feat`: A new feature.
- `fix`: A bug fix.
- `docs`: Documentation changes.
- `style`: Formatting, missing semi colons, etc. (no code change).
- `refactor`: Refactoring code without changing functionality.
- `perf`: Performance improvements.
- `chore`: Maintenance tasks, dependency bumps, or tool configs.

Example:
```
feat(player): add volume normalization toggle
fix(equalizer): prevent duplicate presets on app update
```

---

## 6. Contribution Workflow

Follow this step-by-step submission flow:

1. **Fork** the repository to your own GitHub account.
2. **Clone** your fork locally:
    ```bash
    git clone https://github.com/<your-username>/Denge-MusicApp.git
    ```
3. **Create a branch** off `main` using the branch naming convention:
    ```bash
    git checkout -b feat/my-new-feature
    ```
4. **Make changes** while following [docs/Rules.md](docs/Rules.md).
5. **Verify your build** locally:
    ```bash
    ./gradlew assembleDebug
    ./gradlew lintDebug
    ```
6. **Commit** your changes using Conventional Commits.
7. **Push** to your fork:
    ```bash
    git push origin feat/my-new-feature
    ```
8. **Submit a Pull Request (PR)** targeting the `main` branch of the upstream repository.
9. Complete all checklist items in the Pull Request template.
