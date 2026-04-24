# AiModelHub – Copilot Instructions

## Build & Run Commands

```bash
# Build all modules
./gradlew assembleDebug

# Build a single module
./gradlew :app:assembleDebug
./gradlew :sdk:assembleDebug
./gradlew :demo:assembleDebug

# Run unit tests (app module)
./gradlew :app:test
# Run a single test class
./gradlew :app:testDebugUnitTest --tests "com.ai_model_hub.ExampleUnitTest"

# Run instrumented tests
./gradlew :app:connectedDebugAndroidTest

# Lint
./gradlew :app:lint

# Clean build
./gradlew clean assembleDebug
```

## Architecture

The project is split into three Gradle modules:

```
:app    – Main Android application (Compose UI + MVVM + Hilt + WorkManager)
:sdk    – Android library exposing AiHubClient; the only place AIDL lives
:demo   – Sample app that binds to :app's service via :sdk
```

### Data flow

```
ModelAllowlist (static registry)
    ↓ read by
ModelManagerViewModel  ──→  AppRepository (DataStore)
    ↓ triggers                  ↑ persists downloaded/enabled sets
DownloadRepository (WorkManager)
    ↓ on success
LiteRtLmHelper  ──→  LiteRT LM Engine (model.instance: LlmInstance)
    ↑ called by
AiModelHubService (AIDL Bound Service)  ←── AiHubClient (:sdk)  ←── :demo / 3rd-party apps
ChatViewModel  (also calls LiteRtLmHelper directly, in-process)
```

### AIDL boundary
- AIDL definitions (`IAiModelHubService.aidl`, `IAiResponseCallback.aidl`) live **only** in `:sdk/src/main/aidl/`.
- `:app` consumes the generated stubs via `implementation(project(":sdk"))` — do **not** add AIDL files to `:app`.
- `:demo` also depends only on `:sdk`.

### Model lifecycle
1. `ModelAllowlist` defines all supported models (name, URL, file name, size).
2. `DownloadWorker` (WorkManager, foreground type `dataSync`) downloads to `externalFilesDir/{normalizedName}/{version}/{fileName}`.  
   Downloads are resumable: a `.tmp` suffix is used and renamed on completion.
3. `AppRepository` (DataStore) tracks which model names are in `downloaded_models` and `enabled_models` sets.
4. `LiteRtLmHelper` wraps the LiteRT LM `Engine`/`Conversation` API. The live engine is stored in `model.instance` as a `LlmInstance`.
5. Deleting a model (`markModelDeleted`) automatically disables it (`setModelEnabled(..., false)`).

### Navigation
Two-tab bottom nav (`Models` / `Chat`) managed in `NavGraph.kt`.  
Chat tab is only enabled after a model is selected from Models screen (`currentChatModel` state).  
Route: `chat/{modelName}` — use `Routes.chat(modelName)` helper.

## Key Conventions

### AGP 9 plugin rules
- **Do not** explicitly declare `kotlin.android` in any module's `build.gradle.kts`; AGP 9.1 applies it automatically. Adding it causes a "already on classpath" build error.
- The `android.library` plugin must be declared with `.apply(false)` in the **root** `build.gradle.kts` before it can be used in submodules (same pattern as `android.application`).
- The `:sdk` module has no Compose; adding `kotlin.compose` plugin to it causes "Compose Runtime not found" error.

### Hilt
- All `@HiltViewModel` constructors that receive `Context` must use the **`@param:ApplicationContext`** use-site target (not `@ApplicationContext`) to avoid a Kotlin compiler warning about annotations applying to the backing field.
- Import `hiltViewModel()` from `androidx.hilt.lifecycle.viewmodel.compose` (artifact `hilt-navigation-compose:1.3.0`), not the old `androidx.hilt.navigation.compose` package.

### Compose icon imports
- Use `Icons.AutoMirrored.Filled.*` for directional icons (Chat, Send, etc.) — `Icons.Filled.Chat` / `Icons.Filled.Send` are deprecated.
- Import path: `androidx.compose.material.icons.automirrored.filled.*`

### XNNPack cache
- The XNNPack weight cache **must** be written to `externalFilesDir("xnnpack_cache")`, not the internal `cacheDir`. Internal storage typically has only tens of MB free; LiteRT needs ~1.5 GB.
- `LiteRtLmHelper.initialize()` pre-checks available space and calls `onDone(errorMsg)` instead of crashing if space is insufficient.
- XNNPack calls `abort()` (uncatchable `SIGABRT`) if cache writes fail — never point the cache at a full partition.

### Model name normalization
- `Model.normalizedName` is derived automatically in `init {}` by replacing any non-alphanumeric character with `_`. Use this for file-system paths, not the display name.

### DataStore keys (AppRepository)
| Key | Type | Purpose |
|-----|------|---------|
| `downloaded_models` | `stringSetPreferencesKey` | Set of downloaded model names |
| `enabled_models` | `stringSetPreferencesKey` | Set of enabled model names |

### WorkManager foreground service
`AndroidManifest.xml` must override `SystemForegroundService` with `android:foregroundServiceType="dataSync"` (via `tools:node="merge"`) on Android 14+, and `FOREGROUND_SERVICE_DATA_SYNC` permission must be declared. `DownloadWorker.getForegroundInfo()` returns `ForegroundInfo` with `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`.

### Adding a new model
Edit `ModelAllowlist.kt` only — add a new `Model(...)` entry with the correct `url`, `downloadFileName`, `version`, `sizeInBytes`, and `huggingFaceRepo`. No other files need to change.

## Module Details

| Module | Package | Plugins |
|--------|---------|---------|
| `:app` | `com.ai_model_hub` | `android.application`, `kotlin.compose`, `hilt.application`, `ksp` |
| `:sdk` | `com.ai_model_hub.sdk` | `android.library` only (no Compose, no Hilt) |
| `:demo` | `com.ai_model_hub.demo` | `android.application`, `kotlin.compose` |

- `compileSdk = 36`, `minSdk = 31`, `targetSdk = 36`, `jvmTarget = JVM_17` across all modules.
- All dependency versions are managed in `gradle/libs.versions.toml`.
