# AiModelHub

一款 Android 应用，用于管理端侧 AI 语言模型，并通过系统服务对外提供推理能力。模型完全在设备本地运行，基于 Google 的 [LiteRT LM](https://ai.google.dev/edge/litert) 推理引擎，推理时无需网络连接。

[English](README.md)

---

## 功能特性

- **模型管理** — 浏览、下载、删除来自 Hugging Face 的 LiteRT LM 模型
- **应用内聊天** — 直接在应用中与已下载的模型对话
- **启用/禁用切换** — 启用模型后即可通过绑定服务对外提供推理；状态持久化保存，重启后不丢失
- **AIDL 绑定服务** — 通过稳定的 IPC 接口将已加载模型暴露给第三方应用
- **SDK 库（`:sdk`）** — 封装好的 Kotlin 客户端（`AiHubClient`），方便第三方集成
- **Demo 应用（`:demo`）** — 展示 SDK 完整用法的最小示例应用

---

## 系统要求

| | 最低要求 |
|---|---|
| Android 版本 | 12（API 31） |
| 处理器架构 | arm64-v8a |
| 可用存储空间 | Gemma 4 E2B 约 2.7 GB · Gemma 4 E4B 约 3.9 GB |
| 运行内存 | 建议 4 GB 以上 |

> **XNNPack 缓存说明：** 模型初始化时会将约 1.5 GB 的权重缓存写入外部存储。加载模型前，请确保外部存储分区有至少 1.5 GB 可用空间。

---

## 项目结构

```
AiModelHub/
├── app/          # 主应用（模型管理页 + 聊天页）
├── sdk/          # Android 库模块 — AiHubClient + AIDL 存根
├── demo/         # 展示 SDK 集成的示例应用
└── gradle/
    └── libs.versions.toml   # 统一依赖版本管理
```

### 模块依赖关系

```
:demo ──┐
        ├──→ :sdk ──→ (生成 AIDL 存根)
:app  ──┘
```

---

## 架构说明

### `:app` 分层结构

```
UI 层 (Compose)
  ModelManagerScreen  ──→  ModelManagerViewModel
  ChatScreen          ──→  ChatViewModel
        │                         │
        ▼                         ▼
  AppRepository              LiteRtLmHelper
  (DataStore 持久化)          (LiteRT LM 推理引擎)
        │
  DownloadRepository
  (WorkManager 下载)
        │
  AiModelHubService  ◀── 由 :sdk / :demo 绑定
  (AIDL 绑定服务)
```

- **`ModelAllowlist`** — 所有支持模型的唯一来源；新增模型只需修改此文件。
- **`AppRepository`** — 基于 DataStore，持久化已下载与已启用的模型集合。
- **`DownloadWorker`** — 支持断点续传的 `CoroutineWorker`，含下载进度上报和前台通知。
- **`LiteRtLmHelper`** — 封装 LiteRT LM SDK 的 `Engine` / `Conversation` API，将运行中的引擎以 `LlmInstance` 形式存储在 `model.instance` 中。
- **`AiModelHubService`** — AIDL `Stub` 实现，管理已加载模型实例，并将推理 token 流式返回给调用方。

### `:sdk`

对外暴露两部分内容：
1. **AIDL 接口定义**（`IAiModelHubService`、`IAiResponseCallback`）— 唯一存放位置。
2. **`AiHubClient`** — Kotlin 封装类，处理 `bindService` 绑定，暴露 `connectionState: StateFlow<ConnectionState>`，并将流式回调转换为 Kotlin `Flow<String>`。

---

## 快速开始

### 1. 克隆并构建

```bash
git clone <仓库地址>
cd AiModelHub
./gradlew assembleDebug
```

若要体验完整的 Demo，需同时安装主应用和示例应用：

```bash
./gradlew :app:installDebug
./gradlew :demo:installDebug
```

### 2. 下载模型

打开 **AiModelHub**，进入 **Models** 标签页，点击目标模型的 **下载** 按钮。下载支持断点续传，关闭应用后重新打开可继续下载。

### 3. 启用模型

下载完成后，在模型卡片上点击 **启用**，该模型即可通过 AIDL 服务对外提供服务。

### 4. 应用内聊天

在已启用的模型卡片上点击 **聊天**，进入应用内聊天界面。

---

## 新增模型

只需修改 **`ModelAllowlist.kt`** 一个文件：

```kotlin
Model(
    name             = "My Model",
    displayName      = "My Model 1B",
    description      = "在 UI 中显示的简短描述。",
    url              = "https://huggingface.co/org/repo/resolve/main/model.litertlm",
    sizeInBytes      = 1_200_000_000L,
    downloadFileName = "model.litertlm",
    version          = "main",
    huggingFaceRepo  = "org/repo",
)
```

其他文件无需修改。

---

## SDK 集成指南

第三方 Android 应用可以通过 `:sdk` 模块调用 AiModelHub 的推理能力，无需自行打包任何模型权重文件。

> **注意：** GitHub Packages 即使对公开仓库也需要身份验证。你需要一个具有 `read:packages` 权限的 [GitHub Personal Access Token](https://github.com/settings/tokens)。

### 1. 配置凭据

将你的 GitHub 用户名和 Token 添加到 `~/.gradle/gradle.properties`（**不要提交到代码仓库**）：

```properties
# ~/.gradle/gradle.properties
gpr.user=你的GitHub用户名
gpr.key=你的GitHub_Token
```

### 2. 添加仓库和依赖

在 `settings.gradle.kts` 中：

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

然后添加 SDK 依赖：

```kotlin
// app/build.gradle.kts
implementation("com.ai_model_hub:sdk:0.1.0")
```

> 将 `0.1.0` 替换为[最新发布版本](https://github.com/alex-80/AiModelHub/releases)。

### 3. 在 AndroidManifest.xml 中声明权限和查询

```xml
<!-- 绑定 AiModelHubService 所需权限 -->
<uses-permission android:name="com.ai_model_hub.permission.BIND_SERVICE" />

<queries>
    <package android:name="com.ai_model_hub" />
    <!-- 同时声明服务 action，确保在 OEM 定制 ROM 上
         bindService 的包可见性检查能顺利通过。-->
    <intent>
        <action android:name="com.ai_model_hub.service.AiModelHubService" />
    </intent>
</queries>
```

### 4. 使用 `AiHubClient`

```kotlin
class MyViewModel(app: Application) : AndroidViewModel(app) {

    private val client = AiHubClient(app.applicationContext)

    init {
        // 监听连接状态
        viewModelScope.launch {
            client.connectionState.collect { state ->
                when (state) {
                    ConnectionState.Disconnected -> { /* 显示连接按钮 */ }
                    ConnectionState.Connecting   -> { /* 显示加载中 */ }
                    is ConnectionState.Connected -> { /* 已就绪 */ }
                    is ConnectionState.Error     -> { /* 显示错误信息 */ }
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
                .collect { token -> /* 将 token 追加到 UI */ }
        }
    }

    override fun onCleared() {
        client.disconnect()
    }
}
```

### AIDL 接口参考

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
void onToken(String token);        // 增量 token
void onComplete(String fullText);  // 生成完成
void onError(String errorMessage); // 发生错误
```

---

## 构建命令

```bash
# 构建所有模块
./gradlew assembleDebug

# 构建单个模块
./gradlew :app:assembleDebug
./gradlew :sdk:assembleDebug
./gradlew :demo:assembleDebug

# 单元测试
./gradlew :app:test

# Lint 检查
./gradlew :app:lint

# 清理构建
./gradlew clean
```

---

## 技术栈

| 组件 | 库 / 版本 |
|------|-----------|
| 开发语言 | Kotlin 2.3.21 |
| UI 框架 | Jetpack Compose · Material3 |
| 依赖注入 | Hilt 2.59.2 |
| 页面导航 | Navigation Compose 2.9.8 |
| 后台任务 | WorkManager 2.11.2 |
| 数据持久化 | DataStore Preferences 1.2.1 |
| AI 推理 | LiteRT LM 0.10.2 |
| 构建工具 | AGP 9.1.1 · KSP 2.3.6 |
| 最低 SDK | 31（Android 12） |

---

## 许可证

本项目仅供学习和演示使用。
