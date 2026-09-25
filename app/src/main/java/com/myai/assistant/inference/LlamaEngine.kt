package com.myai.assistant.inference

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Kotlin-side bridge to the native llama.cpp inference engine
 * (see app/src/main/cpp/llama_bridge.cpp).
 *
 * This is a real, working JNI boundary and native implementation against the
 * llama.cpp public API, but it has NOT been validated end-to-end against a
 * running model on physical hardware as part of this change \u2014 there was no
 * Android device/emulator or network access available while writing this.
 * The llama.cpp API also moves between versions; if the pinned commit CI
 * fetches (see .github/workflows/android-build.yml) has renamed any of the
 * functions used in llama_bridge.cpp, that file will need small adjustments.
 * Treat the first CI build + first on-device run as a debugging pass, not a
 * guarantee.
 */
class LlamaEngine {

    private var nativeHandle: Long = 0L

    external fun nativeLoadModel(modelPath: String, contextLength: Int, nThreads: Int): Long
    external fun nativeFreeModel(handle: Long)
    external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int, callback: TokenCallback)
    external fun nativeStop(handle: Long)

    fun interface TokenCallback {
        fun onToken(token: String): Boolean // return false to stop generation
    }

    val isLoaded: Boolean get() = nativeHandle != 0L

    fun load(
        modelPath: String,
        contextLength: Int = 4096,
        nThreads: Int = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
    ) {
        unload()
        nativeHandle = nativeLoadModel(modelPath, contextLength, nThreads)
    }

    fun unload() {
        if (nativeHandle != 0L) {
            nativeFreeModel(nativeHandle)
            nativeHandle = 0L
        }
    }

    fun generate(prompt: String, maxTokens: Int = 512): Flow<String> = callbackFlow {
        if (nativeHandle == 0L) {
            close(IllegalStateException("Model not loaded"))
            return@callbackFlow
        }
        val handle = nativeHandle
        val worker = Thread {
            val callback = TokenCallback { token -> trySend(token).isSuccess }
            nativeGenerate(handle, prompt, maxTokens, callback)
            close()
        }
        worker.start()
        awaitClose {
            nativeStop(handle)
            worker.interrupt()
        }
    }

    companion object {
        init {
            System.loadLibrary("myai_llama_bridge")
        }
    }
}
