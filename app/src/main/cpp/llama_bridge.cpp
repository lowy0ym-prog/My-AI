// Real JNI bridge to llama.cpp's public C API. This targets the llama.cpp
// checkout that CI clones at build time (see ../../../.github/workflows/
// android-build.yml). llama.cpp's API surface moves between versions; if the
// commit CI fetches has renamed any symbol used below, this file needs a
// small update to match. It has not been exercised against a live model on
// device as part of this change.

#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <algorithm>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "MyAiLlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct EngineHandle {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    std::atomic<bool> stop_requested{false};
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeLoadModel(
        JNIEnv* env, jobject /*thiz*/, jstring modelPath, jint contextLength, jint nThreads) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    llama_model* model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextLength;
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;

    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_free_model(model);
        return 0;
    }

    auto* handle = new EngineHandle();
    handle->model = model;
    handle->ctx = ctx;
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT void JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeFreeModel(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong handlePtr) {
    auto* handle = reinterpret_cast<EngineHandle*>(handlePtr);
    if (!handle) return;
    if (handle->ctx) llama_free(handle->ctx);
    if (handle->model) llama_free_model(handle->model);
    delete handle;
}

extern "C" JNIEXPORT void JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeStop(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong handlePtr) {
    auto* handle = reinterpret_cast<EngineHandle*>(handlePtr);
    if (handle) handle->stop_requested = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeGenerate(
        JNIEnv* env, jobject /*thiz*/, jlong handlePtr, jstring prompt, jint maxTokens, jobject callback) {
    auto* handle = reinterpret_cast<EngineHandle*>(handlePtr);
    if (!handle || !handle->ctx) return;
    handle->stop_requested = false;

    const char* promptChars = env->GetStringUTFChars(prompt, nullptr);
    std::string promptStr(promptChars);
    env->ReleaseStringUTFChars(prompt, promptChars);

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)Z");

    const llama_model* model = handle->model;
    std::vector<llama_token> tokens(promptStr.size() + 8);
    int nTokens = llama_tokenize(model, promptStr.c_str(), (int32_t) promptStr.size(),
                                  tokens.data(), (int32_t) tokens.size(), true, true);
    tokens.resize(std::max(nTokens, 0));

    if (llama_decode(handle->ctx, llama_batch_get_one(tokens.data(), (int32_t) tokens.size())) != 0) {
        LOGE("Initial decode failed");
        return;
    }

    int nVocab = llama_n_vocab(model);
    for (int i = 0; i < maxTokens && !handle->stop_requested; ++i) {
        float* logits = llama_get_logits(handle->ctx);

        // Greedy sampling only. Swap in temperature/top-p/top-k (or
        // llama.cpp's sampler API) once basic generation is validated.
        llama_token bestToken = 0;
        float bestLogit = logits[0];
        for (int t = 1; t < nVocab; ++t) {
            if (logits[t] > bestLogit) {
                bestLogit = logits[t];
                bestToken = t;
            }
        }

        if (llama_token_is_eog(model, bestToken)) break;

        char buf[256];
        int len = llama_token_to_piece(model, bestToken, buf, sizeof(buf), 0, true);
        std::string piece(buf, std::max(len, 0));

        jstring jpiece = env->NewStringUTF(piece.c_str());
        jboolean shouldContinue = env->CallBooleanMethod(callback, onTokenMethod, jpiece);
        env->DeleteLocalRef(jpiece);
        if (!shouldContinue) break;

        llama_token nextTokenArr[1] = {bestToken};
        if (llama_decode(handle->ctx, llama_batch_get_one(nextTokenArr, 1)) != 0) {
            LOGE("Decode step failed");
            break;
        }
    }
}
