package com.ai_model_hub.data

import android.content.Context
import com.ai_model_hub.sdk.Model
import java.io.File

fun Model.getModelDir(context: Context): String {
    return listOf(
        context.getExternalFilesDir(null)?.absolutePath ?: "",
        normalizedName,
        version
    ).joinToString(File.separator)
}

fun Model.getModelFilePath(context: Context): String {
    val baseDir = getModelDir(context)
    return listOf(baseDir, downloadFileName).joinToString(File.separator)
}