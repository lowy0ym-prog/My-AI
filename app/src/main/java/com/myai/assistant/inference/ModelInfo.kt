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
 * run reasonably well on modern Android phones (6-8GB+ RAM recommended) via
 * llama.cpp. Users can also import their own GGUF file from Models > Import.
 *
 * Llama 3.2 and Phi-3.5 point at bartowski's community GGUF re-quantizations
 * (verified via web search, not gated behind a Hugging Face login) rather
 * than the original Meta/Microsoft repos, which either gate the download
 * behind license acceptance or don't publish GGUF files directly. Same
 * underlying weights, same license terms as the original model either way.
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
            approxSizeGb = 2.02,
            contextLength = 8192,
            minRamGb = 4.0,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            licenseNote = "Llama 3.2 Community License (community GGUF re-quantization by bartowski; same license terms as Meta's original model)."
        ),
        ModelInfo(
            id = "phi-3.5-mini-instruct-q4_k_m",
            displayName = "Phi-3.5 Mini Instruct (Q4_K_M)",
            quantization = "Q4_K_M",
            approxSizeGb = 2.39,
            contextLength = 4096,
            minRamGb = 4.0,
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            licenseNote = "MIT license."
        )
    )
}
