#include <jni.h>
#include <memory>
#include <vector>

#include "playback_engine.h"

namespace {
inline macsense::PlaybackEngine* toEngine(jlong handle) {
    return reinterpret_cast<macsense::PlaybackEngine*>(handle);
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new macsense::PlaybackEngine());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativeLoadPcm(
    JNIEnv* env, jobject, jlong handle, jfloatArray samples, jint sampleRate) {
    auto* engine = toEngine(handle);
    if (engine == nullptr) return JNI_FALSE;

    jsize length = env->GetArrayLength(samples);
    std::vector<float> buffer(static_cast<size_t>(length));
    env->GetFloatArrayRegion(samples, 0, length, buffer.data());

    bool ok = engine->loadPcm(buffer.data(), length, sampleRate);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativePlay(JNIEnv*, jobject, jlong handle) {
    auto* engine = toEngine(handle);
    return (engine != nullptr && engine->play()) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativePause(JNIEnv*, jobject, jlong handle) {
    auto* engine = toEngine(handle);
    return (engine != nullptr && engine->pause()) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativeStop(JNIEnv*, jobject, jlong handle) {
    auto* engine = toEngine(handle);
    return (engine != nullptr && engine->stop()) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativeSeekToFrame(
    JNIEnv*, jobject, jlong handle, jlong frame) {
    auto* engine = toEngine(handle);
    if (engine != nullptr) engine->seekToFrame(frame);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativeGetPositionFrames(
    JNIEnv*, jobject, jlong handle) {
    auto* engine = toEngine(handle);
    return engine != nullptr ? engine->getPositionFrames() : 0L;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativeGetTotalFrames(
    JNIEnv*, jobject, jlong handle) {
    auto* engine = toEngine(handle);
    return engine != nullptr ? engine->getTotalFrames() : 0L;
}

extern "C" JNIEXPORT void JNICALL
Java_com_macsense_ai_audio_NativePlaybackEngine_nativeRelease(JNIEnv*, jobject, jlong handle) {
    auto* engine = toEngine(handle);
    delete engine;
}
