package com.myai.assistant.inference

data class ModelInfo(
    val id: String,
    val displayName: String,
    val quantization: String,
    val approxSizeGb: Double,
    val contextLength: Int,
    val minRamGb: Double,
    val downloadUrl: String,
    val supportsTools: Boolean = false,
    val licenseNote: String
)

/**
 * A small starter catalog of practical open-weight, GGUF-quantized models that
 * run reasonably well on modern Android phones (8GB+ RAM recommended) via
 * llama.cpp. Users can also import their own GGUF file from Models > Import.
 *
 * IMPORTANT: the downloadUrl values below point at the expected Hugging Face
 * GGUF repos for each model family but have not been individually verified
 * as part of this change (no network access was available while building
 * this scaffold). Confirm each URL resolves to a real file before shipping,
 * and swap in a specific verified quantization if the filename differs.
 */
object ModelCatalog {
    val recommended = ModelInfo(
        id = "qwen2.5-3b-instruct-q4_k_m",
        displayName = "Qwen2.5 3B Instruct (Q4_K_M)",
        quantization = "Q4_K_M",
        approxSizeGb = 2.0,
        contextLength = 8192,
        minRamGb = 4.0,
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
        licenseNote = "Qwen license \u2014 verify terms on the model page before redistribution."
    )

    val all = listOf(
        recommended,
        ModelInfo(
            id = "llama-3.2-3b-instruct-q4_k_m",
            displayName = "Llama 3.2 3B Instruct (Q4_K_M)",
            quantization = "Q4_K_M",
            approxSizeGb = 2.0,
            contextLength = 8192,
            minRamGb = 4.0,
            downloadUrl = "https://huggingface.co/meta-llama/Llama-3.2-3B-Instruct-GGUF/resolve/main/llama-3.2-3b-instruct-q4_k_m.gguf",
            licenseNote = "Llama 3.2 Community License \u2014 requires accepting Meta's terms on Hugging Face."
        ),
        ModelInfo(
            id = "phi-3.5-mini-instruct-q4_k_m",
            displayName = "Phi-3.5 Mini Instruct (Q4_K_M)",
            quantization = "Q4_K_M",
            approxSizeGb = 2.2,
            contextLength = 4096,
            minRamGb = 4.0,
            downloadUrl = "https://huggingface.co/microsoft/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            licenseNote = "MIT license."
        )
    )
}
