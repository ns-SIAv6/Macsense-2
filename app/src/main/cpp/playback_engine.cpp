#include "playback_engine.h"

#include <algorithm>
#include <android/log.h>

#define LOG_TAG "MacsensePlayback"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace macsense {

namespace {
aaudio_data_callback_result_t dataCallbackTrampoline(
    AAudioStream* stream, void* userData, void* audioData, int32_t numFrames) {
    auto* engine = static_cast<PlaybackEngine*>(userData);
    return engine->onAudioReady(stream, audioData, numFrames);
}

void errorCallbackTrampoline(AAudioStream* /*stream*/, void* /*userData*/, aaudio_result_t error) {
    LOGE("AAudio stream error: %d", error);
}
} // namespace

PlaybackEngine::~PlaybackEngine() { release(); }

bool PlaybackEngine::loadPcm(const float* samples, int32_t frameCount, int32_t sampleRate) {
    if (frameCount < 0 || sampleRate <= 0) return false;
    release();
    pcm_.assign(samples, samples + frameCount);
    sampleRate_ = sampleRate;
    readFrame_.store(0, std::memory_order_release);
    return true;
}

bool PlaybackEngine::openStream() {
    if (stream_ != nullptr) return true;
    if (pcm_.empty()) {
        LOGE("openStream called with no PCM loaded");
        return false;
    }

    AAudioStreamBuilder* builder = nullptr;
    if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK || builder == nullptr) {
        LOGE("Failed to create AAudio stream builder");
        return false;
    }

    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setSampleRate(builder, sampleRate_);
    AAudioStreamBuilder_setChannelCount(builder, 1);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setDataCallback(builder, dataCallbackTrampoline, this);
    AAudioStreamBuilder_setErrorCallback(builder, errorCallbackTrampoline, this);

    aaudio_result_t result = AAudioStreamBuilder_openStream(builder, &stream_);
    AAudioStreamBuilder_delete(builder);

    if (result != AAUDIO_OK || stream_ == nullptr) {
        LOGE("Failed to open exclusive low-latency stream (%d), retrying shared", result);
        // Exclusive mode can be denied on some devices/OEMs; fall back to shared.
        if (AAudio_createStreamBuilder(&builder) != AAUDIO_OK || builder == nullptr) return false;
        AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
        AAudioStreamBuilder_setSampleRate(builder, sampleRate_);
        AAudioStreamBuilder_setChannelCount(builder, 1);
        AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
        AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
        AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
        AAudioStreamBuilder_setDataCallback(builder, dataCallbackTrampoline, this);
        AAudioStreamBuilder_setErrorCallback(builder, errorCallbackTrampoline, this);
        result = AAudioStreamBuilder_openStream(builder, &stream_);
        AAudioStreamBuilder_delete(builder);
        if (result != AAUDIO_OK || stream_ == nullptr) {
            LOGE("Failed to open shared fallback stream (%d)", result);
            stream_ = nullptr;
            return false;
        }
    }
    return true;
}

bool PlaybackEngine::play() {
    if (!openStream()) return false;
    aaudio_result_t result = AAudioStream_requestStart(stream_);
    if (result != AAUDIO_OK) {
        LOGE("requestStart failed: %d", result);
        return false;
    }
    return true;
}

bool PlaybackEngine::pause() {
    if (stream_ == nullptr) return true;
    aaudio_result_t result = AAudioStream_requestPause(stream_);
    return result == AAUDIO_OK;
}

bool PlaybackEngine::stop() {
    if (stream_ != nullptr) {
        AAudioStream_requestStop(stream_);
    }
    readFrame_.store(0, std::memory_order_release);
    return true;
}

void PlaybackEngine::seekToFrame(int64_t frame) {
    int64_t clamped = std::clamp<int64_t>(frame, 0, static_cast<int64_t>(pcm_.size()));
    readFrame_.store(clamped, std::memory_order_release);
}

int64_t PlaybackEngine::getPositionFrames() const {
    return readFrame_.load(std::memory_order_acquire);
}

void PlaybackEngine::release() {
    if (stream_ != nullptr) {
        AAudioStream_requestStop(stream_);
        AAudioStream_close(stream_);
        stream_ = nullptr;
    }
}

aaudio_data_callback_result_t PlaybackEngine::onAudioReady(
    AAudioStream* /*stream*/, void* audioData, int32_t numFrames) {
    auto* out = static_cast<float*>(audioData);
    int64_t start = readFrame_.load(std::memory_order_acquire);
    int64_t total = static_cast<int64_t>(pcm_.size());
    int32_t framesWritten = 0;

    for (; framesWritten < numFrames; framesWritten++) {
        int64_t idx = start + framesWritten;
        out[framesWritten] = (idx < total) ? pcm_[static_cast<size_t>(idx)] : 0.0f;
    }

    int64_t newPos = start + framesWritten;
    if (newPos >= total) {
        readFrame_.store(total, std::memory_order_release);
        return AAUDIO_CALLBACK_RESULT_STOP;
    }
    readFrame_.store(newPos, std::memory_order_release);
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

} // namespace macsense
