package com.ai_model_hub.data

import android.content.Context
import com.ai_model_hub.data.remote.RemoteModel
import java.io.File

private val NORMALIZE_NAME_REGEX = Regex("[^a-zA-Z0-9]")

fun RemoteModel.getModelDir(context: Context): String {
    val normalizedName = NORMALIZE_NAME_REGEX.replace(name, "_")
    val version = commitHash.take(7)
    return listOf(
        context.getExternalFilesDir(null)?.absolutePath ?: "",
        normalizedName,
        version
    ).joinToString(File.separator)
}

fun RemoteModel.getModelFilePath(context: Context): String {
    val baseDir = getModelDir(context)
    return listOf(baseDir, modelFile).joinToString(File.separator)
}
