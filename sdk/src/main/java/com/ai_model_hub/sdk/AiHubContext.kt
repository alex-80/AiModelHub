package com.ai_model_hub.sdk

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
internal object AiHubContext {

    lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}