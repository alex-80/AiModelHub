package com.ai_model_hub.sdk

/**
 * Preferred compute backend for model inference.
 * Each [Model] declares which backends it supports via [Model.supportedBackends].
 * The actual backend used is the intersection of the user preference and the model's
 * supported list — no runtime fallback or exception handling is needed.
 */
enum class BackendPreference {
    CPU,
    GPU,
}
