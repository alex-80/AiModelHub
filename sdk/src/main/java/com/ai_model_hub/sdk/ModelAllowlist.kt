package com.ai_model_hub.sdk

object ModelAllowlist {
    val models: List<Model> = listOf(
        Model(
            name = ModelName.GEMMA_4_E2B,
            displayName = "Gemma 4 E2B",
            description = "Gemma 4 E2B instruction-tuned model (2.58 GB). Multimodal edge model supporting text, vision and audio. Supports up to 32k context length.",
            url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            sizeInBytes = 2_710_000_000L,
            downloadFileName = "gemma-4-E2B-it.litertlm",
            version = "main",
            huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm",
            maxTokens = 4000,
        ),
        Model(
            name = ModelName.GEMMA_4_E4B,
            displayName = "Gemma 4 E4B",
            description = "Gemma 4 E4B instruction-tuned model (3.65 GB). Higher-quality multimodal edge model supporting text, vision and audio. Supports up to 32k context length.",
            url = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            sizeInBytes = 3_830_000_000L,
            downloadFileName = "gemma-4-E4B-it.litertlm",
            version = "main",
            huggingFaceRepo = "litert-community/gemma-4-E4B-it-litert-lm",
            maxTokens = 4000,
        ),
    )

    fun findByName(name: String): Model? = models.find { it.name == name }
}
