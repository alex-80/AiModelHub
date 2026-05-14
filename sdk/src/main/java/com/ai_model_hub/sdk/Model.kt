package com.ai_model_hub.sdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Model(
    val name: String,
    val displayName: String = "",
    val description: String = "",
    val modelId: String = "",
) : Parcelable

