package com.ai_model_hub.extension

fun Long.formatFileSize(): String = when {
    this >= 1_000_000_000L -> "%.1f GB".format(this / 1_000_000_000.0)
    this >= 1_000_000L -> "%.1f MB".format(this / 1_000_000.0)
    this >= 1_000L -> "%.1f KB".format(this / 1_000.0)
    else -> "$this B"
}