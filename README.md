# AiModelHub

An Android application for managing on-device AI language models and exposing them as a system-wide service to other apps. Models run entirely on-device using Google's [LiteRT LM](https://ai.google.dev/edge/litert) inference engine — no internet connection required at inference time.

[中文文档](README.zh-CN.md)

---

## Features

- **Model Manager** — browse, download, and delete LiteRT LM models from Hugging Face
- **In-app Chat** — converse with any downloaded model directly inside the app
- **Enabled/Disabled toggle** — enable a model to make it available to the bound service; state is persisted across restarts
- **AIDL Bound Service** — exposes loaded models to any third-party app via a stable IPC interface
- **SDK library (`:sdk`)** — a ready-made Kotlin client (`AiHubClient`) for integrating with the service
- **Demo app (`:demo`)** — a minimal sample showing end-to-end SDK usage

---

## Requirements

| | Minimum |
|---|---|
| Android | 12 (API 31) |
| Architecture | arm64-v8a |
| Free storage | ~2.7 GB for Gemma 4 E2B · ~3.9 GB for Gemma 4 E4B |
| RAM | 4 GB recommended |

> **XNNPack cache note:** model initialization writes a ~1.5 GB weight cache to external storage. Ensure at least 1.5 GB free on the external partition before loading a model.

---

## Project Structure

```
AiModelHub/
├── app/          # Main application (Models screen + Chat screen)
├── sdk/          # Android library — AiHubClient + AIDL stubs
├── demo/         # Sample app demonstrating SDK integration
└── gradle/
    └── libs.versions.toml   # Centralized dependency versions
```

### Module dependency graph

```
:demo ──┐
        ├──→ :sdk ──→ (AIDL stubs generated)
:app  ──┘
```

---

## Architecture

### `:app` layers

```
UI (Compose)
  ModelManagerScreen  ──→  ModelManagerViewModel
  ChatScreen          ──→  ChatViewModel
        │                         │
        ▼                         ▼
  AppRepository              LiteRtLmHelper
  (DataStore)                (LiteRT LM Engine)
        │
  DownloadRepository
  (WorkManager)
        │
  AiModelHubService  ◀── bound by :sdk / :demo
  (AIDL Service)
```

- **`ModelAllowlist`** — single source of truth for all supported models; add new models here only.
- **`AppRepository`** — DataStore-backed persistence for downloaded and enabled model sets.
- **`DownloadWorker`** — `CoroutineWorker` with resumable HTTP download, progress reporting, and foreground notification.
- **`LiteRtLmHelper`** — wraps `Engine` / `Conversation` from the LiteRT LM SDK. Stores the live engine in `model.instance` as `LlmInstance`.
- **`AiModelHubService`** — AIDL `Stub` that manages loaded model instances and streams inference tokens back to callers.

### `:sdk`

Exposes two things:
1. **AIDL definitions** (`IAiModelHubService`, `IAiResponseCallback`) — the only place they live.
2. **`AiHubClient`** — Kotlin wrapper that handles `bindService`, exposes `connectionState: StateFlow<ConnectionState>`, and converts streaming callbacks to a Kotlin `Flow<String>`.

---

## Getting Started

### 1. Clone and build

```bash
git clone <repo-url>
cd AiModelHub
./gradlew assembleDebug
```

Install all three APKs if you want to try the demo:

```bash
./gradlew :app:installDebug
./gradlew :demo:installDebug
```

### 2. Download a model

Open **AiModelHub**, go to the **Models** tab, and tap **Download** on the desired model. Downloads are resumable — you can close the app and re-open it to continue.

### 3. Enable the model

After downloading, toggle **Enable** on the model card. The model is now exposed to the AIDL service.

### 4. Chat in-app

Tap **Chat** on an enabled model card to open the in-app chat screen.

---

## Adding a New Model

Edit **only** `ModelAllowlist.kt`:

```kotlin
Model(
    name          = "My Model",
    displayName   = "My Model 1B",
    description   = "Short description shown in the UI.",
    url           = "https://huggingface.co/org/repo/resolve/main/model.litertlm",
    sizeInBytes   = 1_200_000_000L,
    downloadFileName = "model.litertlm",
    version       = "main",
    huggingFaceRepo = "org/repo",
)
```

No other files need to change.

---

## SDK Integration

Other Android apps can use the `:sdk` module to call AI inference in AiModelHub without bundling any model weights themselves.

> **Note:** GitHub Packages requires authentication even for public repositories. You need a [GitHub Personal Access Token](https://github.com/settings/tokens) with the `read:packages` scope.

### 1. Configure credentials

Add your GitHub username and token to `~/.gradle/gradle.properties` (never commit these to your repo):

```properties
# ~/.gradle/gradle.properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

### 2. Add the repository and dependency

In your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/alex-80/AiModelHub")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
```

Then add the SDK dependency:

```kotlin
// app/build.gradle.kts
implementation("com.ai_model_hub:sdk:0.1.0")
```

> Replace `0.1.0` with the [latest release tag](https://github.com/alex-80/AiModelHub/releases).

### 3. Declare the query in AndroidManifest.xml

```xml
<queries>
    <package android:name="com.ai_model_hub" />
</queries>
```

### 4. Use `AiHubClient`

```kotlin
class MyViewModel(app: Application) : AndroidViewModel(app) {

    private val client = AiHubClient(app.applicationContext)

    init {
        // Observe connection state
        viewModelScope.launch {
            client.connectionState.collect { state ->
                when (state) {
                    ConnectionState.Disconnected -> { /* show connect button */ }
                    ConnectionState.Connecting   -> { /* show spinner */ }
                    is ConnectionState.Connected -> { /* ready */ }
                    is ConnectionState.Error     -> { /* show error */ }
                }
            }
        }
    }

    fun connect() = client.connect()

    fun loadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            client.loadModel("Gemma 4 E2B")
        }
    }

    fun chat(message: String) {
        viewModelScope.launch {
            client.sendMessage("Gemma 4 E2B", message)
                .collect { token -> /* append token to UI */ }
        }
    }

    override fun onCleared() {
        client.disconnect()
    }
}
```

### AIDL interface reference

```java
// IAiModelHubService
List<String> getLoadedModels();
void         loadModel(String modelName);
void         unloadModel(String modelName);
boolean      isModelLoaded(String modelName);
void         sendMessage(String modelName, String message, IAiResponseCallback callback);
void         stopGeneration(String modelName);
void         resetSession(String modelName);

// IAiResponseCallback
void onToken(String token);       // incremental token
void onComplete(String fullText); // generation finished
void onError(String errorMessage);
```

---

## Build Commands

```bash
# Build all modules
./gradlew assembleDebug

# Build a single module
./gradlew :app:assembleDebug
./gradlew :sdk:assembleDebug
./gradlew :demo:assembleDebug

# Unit tests
./gradlew :app:test

# Lint
./gradlew :app:lint

# Clean
./gradlew clean
```

---

## Tech Stack

| Component | Library / Version |
|-----------|-------------------|
| Language | Kotlin 2.3.21 |
| UI | Jetpack Compose · Material3 |
| DI | Hilt 2.59.2 |
| Navigation | Navigation Compose 2.9.8 |
| Background work | WorkManager 2.11.2 |
| Persistence | DataStore Preferences 1.2.1 |
| Inference | LiteRT LM 0.10.2 |
| Build | AGP 9.1.1 · KSP 2.3.6 |
| Min SDK | 31 (Android 12) |

---

## License

This project is provided for demonstration and educational purposes.
