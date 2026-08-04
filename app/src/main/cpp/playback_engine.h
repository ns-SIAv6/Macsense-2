#ifndef MACSENSE_PLAYBACK_ENGINE_H
#define MACSENSE_PLAYBACK_ENGINE_H

#include <aaudio/AAudio.h>
#include <atomic>
#include <cstdint>
#include <vector>

namespace macsense {

// Low-latency mono PCM playback engine built directly on the AAudio NDK API.
//
// Design notes:
// - The whole track is decoded into a float PCM buffer up front by loadPcm();
//   this targets short recorded takes/loops, not long-form streaming media.
// - A single atomic read cursor (readFrame_) is advanced only by the AAudio
//   data callback (the audio thread), and only ever read/reset from the
//   control thread via play()/pause()/seek()/stop(). This keeps the audio
//   callback allocation-free and lock-free, which AAudio requires for
//   glitch-free low-latency operation.
class PlaybackEngine {
public:
    PlaybackEngine() = default;
    ~PlaybackEngine();

    // Loads normalized mono float32 PCM (values expected in [-1, 1]).
    // Safe to call while stopped; returns false if the stream can't be sized.
    bool loadPcm(const float* samples, int32_t frameCount, int32_t sampleRate);

    // Opens (if needed) and starts the AAudio stream from the current position.
    bool play();

    // Pauses the stream; position is preserved for a subsequent play().
    bool pause();

    // Stops the stream and resets the read position to frame 0.
    bool stop();

    // Seeks to an absolute frame offset. Clamped to [0, frameCount].
    void seekToFrame(int64_t frame);

    // Current playhead in frames, safe to poll from any thread.
    int64_t getPositionFrames() const;

    int32_t getSampleRate() const { return sampleRate_; }
    int64_t getTotalFrames() const { return static_cast<int64_t>(pcm_.size()); }

    // Releases the AAudio stream. The engine can be reused after loadPcm().
    void release();

    // Invoked by the AAudio callback trampoline (playback_jni.cpp).
    aaudio_data_callback_result_t onAudioReady(AAudioStream* stream, void* audioData, int32_t numFrames);

private:
    bool openStream();

    std::vector<float> pcm_;
    std::atomic<int64_t> readFrame_{0};
    int32_t sampleRate_ = 44100;
    AAudioStream* stream_ = nullptr;
};

} // namespace macsense

#endif // MACSENSE_PLAYBACK_ENGINE_H
