package com.ai_model_hub.sdk

object ModelAllowlist {
    val models: List<Model> = listOf(
        Model(
            name = ModelName.GEMMA_4_E2B,
            displayName = "Gemma 4 E2B",
            description = "Gemma 4 E2B instruction-tuned model (2.58 GB). Multimodal edge model supporting text, vision and audio. Supports up to 32k context length.",
            modelId = "litert-community/gemma-4-E2B-it-litert-lm",
        ),
        Model(
            name = ModelName.GEMMA_4_E4B,
            displayName = "Gemma 4 E4B",
            description = "Gemma 4 E4B instruction-tuned model (3.65 GB). Higher-quality multimodal edge model supporting text, vision and audio. Supports up to 32k context length.",
            modelId = "litert-community/gemma-4-E4B-it-litert-lm",
        ),
    )

    fun findByName(name: String): Model? = models.find { it.name == name }
}
