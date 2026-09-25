// Fallback native implementation used only when llama.cpp sources were not
// available at build time (see CMakeLists.txt). Lets the app still compile
// and run so the rest of the UI/data layer can be exercised without a real
// model backing it.

#include <jni.h>
#include <string>

extern "C" JNIEXPORT jlong JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeLoadModel(
        JNIEnv*, jobject, jstring, jint, jint) {
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeFreeModel(JNIEnv*, jobject, jlong) {}

extern "C" JNIEXPORT void JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeStop(JNIEnv*, jobject, jlong) {}

extern "C" JNIEXPORT void JNICALL
Java_com_myai_assistant_inference_LlamaEngine_nativeGenerate(
        JNIEnv* env, jobject, jlong, jstring, jint, jobject callback) {
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)Z");
    jstring msg = env->NewStringUTF("[stub build: llama.cpp sources were not fetched - see CI logs]");
    env->CallBooleanMethod(callback, onTokenMethod, msg);
    env->DeleteLocalRef(msg);
}
