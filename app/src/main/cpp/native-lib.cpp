#include <jni.h>
#include <oboe/Oboe.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>
#include <vector>
#include <unordered_map>
#include <memory>
#include <mutex>
#include <cmath>
#include <functional>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class AudioEngine : public oboe::AudioStreamCallback {
private:
    AAssetManager* assetManager_;
    bool isInitialized_;

public:
    AudioEngine(AAssetManager* mgr) : nextSoundId_(1), assetManager_(mgr), isInitialized_(false) {
        oboe::AudioStreamBuilder builder;
        oboe::Result result = builder.setPerformanceMode(oboe::PerformanceMode::LowLatency)
                ->setSharingMode(oboe::SharingMode::Shared)  // Changed from Exclusive to Shared
                ->setFormat(oboe::AudioFormat::Float)
                ->setChannelCount(oboe::ChannelCount::Stereo)
                ->setDirection(oboe::Direction::Output)      // Explicitly set output direction
                ->setCallback(this)
                ->openStream(stream_);

        if (result != oboe::Result::OK) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to open stream: %s",
                                oboe::convertToText(result));
            return;
        }

        __android_log_print(ANDROID_LOG_INFO, "SoundMaker", "Stream opened: SampleRate=%d, Channels=%d, Format=%d", 
                            stream_->getSampleRate(), stream_->getChannelCount(), (int)stream_->getFormat());

        result = stream_->start();
        if (result != oboe::Result::OK) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to start stream: %s",
                                oboe::convertToText(result));
            return;
        }
        
        isInitialized_ = true;
        __android_log_print(ANDROID_LOG_INFO, "SoundMaker", "AudioEngine initialized successfully");
    }

    ~AudioEngine() {
        if (stream_) {
            stream_->stop();
            stream_->close();
        }
    }

    bool isInitialized() const {
        return isInitialized_;
    }

    int32_t loadSound(const char* filename) {
        if (!isInitialized_) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "AudioEngine not initialized, cannot load sound: %s", filename);
            return -1;
        }
        
        AAsset* asset = AAssetManager_open(assetManager_, filename, AASSET_MODE_BUFFER);
        if (!asset) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to open asset: %s", filename);
            return -1;
        }

        // Decode OGG file using Android NDK Media APIs
        off_t length = AAsset_getLength(asset);
        
        // Create a file descriptor from the asset
        off_t start, assetLength;
        int fd = AAsset_openFileDescriptor(asset, &start, &assetLength);
        
        if (fd < 0) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to get file descriptor for: %s", filename);
            AAsset_close(asset);
            return -1;
        }

        // Create MediaExtractor
        AMediaExtractor* extractor = AMediaExtractor_new();
        if (!extractor) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to create MediaExtractor");
            AAsset_close(asset);
            return -1;
        }

        // Set data source
        media_status_t status = AMediaExtractor_setDataSourceFd(extractor, fd, start, assetLength);
        if (status != AMEDIA_OK) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to set data source for: %s", filename);
            AMediaExtractor_delete(extractor);
            AAsset_close(asset);
            return -1;
        }

        // Find audio track
        size_t numTracks = AMediaExtractor_getTrackCount(extractor);
        int audioTrackIndex = -1;
        AMediaFormat* format = nullptr;
        
        for (size_t i = 0; i < numTracks; i++) {
            format = AMediaExtractor_getTrackFormat(extractor, i);
            const char* mime;
            if (AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime)) {
                if (strncmp(mime, "audio/", 6) == 0) {
                    audioTrackIndex = i;
                    break;
                }
            }
            AMediaFormat_delete(format);
            format = nullptr;
        }

        if (audioTrackIndex == -1 || !format) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "No audio track found in: %s", filename);
            AMediaExtractor_delete(extractor);
            AAsset_close(asset);
            return -1;
        }

        // Get audio properties
        int32_t sampleRate, channels;
        AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_SAMPLE_RATE, &sampleRate);
        AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &channels);

        // Select track
        AMediaExtractor_selectTrack(extractor, audioTrackIndex);

        // Create decoder
        const char* mime;
        AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime);
        AMediaCodec* codec = AMediaCodec_createDecoderByType(mime);
        
        if (!codec) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to create decoder for: %s", filename);
            AMediaFormat_delete(format);
            AMediaExtractor_delete(extractor);
            AAsset_close(asset);
            return -1;
        }

        // Configure and start decoder
        status = AMediaCodec_configure(codec, format, nullptr, nullptr, 0);
        if (status != AMEDIA_OK) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to configure decoder");
            AMediaCodec_delete(codec);
            AMediaFormat_delete(format);
            AMediaExtractor_delete(extractor);
            AAsset_close(asset);
            return -1;
        }

        status = AMediaCodec_start(codec);
        if (status != AMEDIA_OK) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to start decoder");
            AMediaCodec_delete(codec);
            AMediaFormat_delete(format);
            AMediaExtractor_delete(extractor);
            AAsset_close(asset);
            return -1;
        }

        // Decode audio data
        std::vector<float> audioData;
        bool inputEOS = false;
        bool outputEOS = false;
        
        while (!outputEOS) {
            // Feed input
            if (!inputEOS) {
                ssize_t bufferIndex = AMediaCodec_dequeueInputBuffer(codec, 0);
                if (bufferIndex >= 0) {
                    size_t bufferSize;
                    uint8_t* buffer = AMediaCodec_getInputBuffer(codec, bufferIndex, &bufferSize);
                    
                    ssize_t sampleSize = AMediaExtractor_readSampleData(extractor, buffer, bufferSize);
                    if (sampleSize > 0) {
                        int64_t presentationTime = AMediaExtractor_getSampleTime(extractor);
                        AMediaCodec_queueInputBuffer(codec, bufferIndex, 0, sampleSize, presentationTime, 0);
                        AMediaExtractor_advance(extractor);
                    } else {
                        AMediaCodec_queueInputBuffer(codec, bufferIndex, 0, 0, 0, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
                        inputEOS = true;
                    }
                }
            }

            // Get output
            AMediaCodecBufferInfo info;
            ssize_t bufferIndex = AMediaCodec_dequeueOutputBuffer(codec, &info, 0);
            
            if (bufferIndex >= 0) {
                if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                    outputEOS = true;
                }
                
                if (info.size > 0) {
                    size_t bufferSize;
                    uint8_t* buffer = AMediaCodec_getOutputBuffer(codec, bufferIndex, &bufferSize);
                    
                    // Convert PCM data to float
                    size_t sampleCount = info.size / sizeof(int16_t);
                    int16_t* samples = reinterpret_cast<int16_t*>(buffer);
                    
                    for (size_t i = 0; i < sampleCount; i++) {
                        float sample = static_cast<float>(samples[i]) / 32768.0f;
                        audioData.push_back(sample);
                    }
                }
                
                AMediaCodec_releaseOutputBuffer(codec, bufferIndex, false);
            }
        }

        // Cleanup
        AMediaCodec_stop(codec);
        AMediaCodec_delete(codec);
        AMediaFormat_delete(format);
        AMediaExtractor_delete(extractor);
        
        __android_log_print(ANDROID_LOG_INFO, "SoundMaker", "Decoded audio: %s, samples: %zu, channels: %d, rate: %d",
                            filename, audioData.size(), channels, sampleRate);

        AAsset_close(asset);

        // Store the sound data
        int soundId = nextSoundId_++;
        SoundData soundData;
        soundData.data = std::move(audioData);
        soundData.channels = channels;
        soundData.sampleRate = sampleRate;
        soundData.frames = audioData.size() / channels;

        sounds_[soundId] = std::move(soundData);

        __android_log_print(ANDROID_LOG_INFO, "SoundMaker", "Sound loaded with ID: %d", soundId);
        return soundId;
    }

    void playSound(int soundId, float volume) {
        if (!isInitialized_) {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "AudioEngine not initialized, cannot play sound ID: %d", soundId);
            return;
        }
        
        auto it = sounds_.find(soundId);
        if (it != sounds_.end()) {
            std::lock_guard<std::mutex> lock(mutex_);
            activeSounds_.push_back({soundId, 0, volume});
            __android_log_print(ANDROID_LOG_DEBUG, "SoundMaker", "Playing sound ID: %d, volume: %.2f", soundId, volume);
        } else {
            __android_log_print(ANDROID_LOG_WARN, "SoundMaker", "Sound ID not found: %d", soundId);
        }
    }

    void stopAllSounds() {
        std::lock_guard<std::mutex> lock(mutex_);
        activeSounds_.clear();
        __android_log_print(ANDROID_LOG_INFO, "SoundMaker", "Stopped all sounds");
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override {
        float* output = static_cast<float*>(audioData);
        int32_t channelCount = stream->getChannelCount();
        int32_t totalSamples = numFrames * channelCount;

        // Clear output buffer
        std::fill(output, output + totalSamples, 0.0f);

        std::lock_guard<std::mutex> lock(mutex_);
        for (auto it = activeSounds_.begin(); it != activeSounds_.end();) {
            auto& activeSound = *it;
            auto& soundData = sounds_[activeSound.soundId];

            int32_t samplesLeft = soundData.data.size() - activeSound.position;
            if (samplesLeft <= 0) {
                it = activeSounds_.erase(it);
                continue;
            }

            int32_t samplesToWrite = std::min(numFrames, samplesLeft / soundData.channels);

            // Mix audio data
            for (int32_t frame = 0; frame < samplesToWrite; ++frame) {
                for (int32_t ch = 0; ch < channelCount; ++ch) {
                    int32_t outputIndex = frame * channelCount + ch;
                    int32_t inputIndex = activeSound.position + frame * soundData.channels;

                    // Handle mono to stereo conversion
                    if (soundData.channels == 1) {
                        output[outputIndex] += soundData.data[inputIndex] * activeSound.volume;
                    } else if (soundData.channels == 2) {
                        int32_t inputChannel = ch < soundData.channels ? ch : 0;
                        output[outputIndex] += soundData.data[inputIndex + inputChannel] * activeSound.volume;
                    }
                }
            }

            activeSound.position += samplesToWrite * soundData.channels;

            if (activeSound.position >= soundData.data.size()) {
                it = activeSounds_.erase(it);
            } else {
                ++it;
            }
        }

        return oboe::DataCallbackResult::Continue;
    }

private:
    struct SoundData {
        std::vector<float> data;
        int channels;
        int sampleRate;
        int64_t frames;
    };

    struct ActiveSound {
        int soundId;
        size_t position;
        float volume;
    };


    std::shared_ptr<oboe::AudioStream> stream_;
    std::unordered_map<int, SoundData> sounds_;
    std::vector<ActiveSound> activeSounds_;
    std::mutex mutex_;
    int nextSoundId_;
};

static std::unique_ptr<AudioEngine> engine = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL Java_my_proj_SoundMaker_initOboe(JNIEnv* env, jobject, jobject assetManager) {
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if (!mgr) {
        __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to get AssetManager");
        return JNI_FALSE;
    }

    try {
        engine = std::make_unique<AudioEngine>(mgr);
        if (engine && engine->isInitialized()) {
            __android_log_print(ANDROID_LOG_INFO, "SoundMaker", "AudioEngine initialized successfully");
            return JNI_TRUE;
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "AudioEngine failed to initialize");
            engine.reset();
            return JNI_FALSE;
        }
    } catch (const std::exception& e) {
        __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to initialize AudioEngine: %s", e.what());
        engine.reset();
        return JNI_FALSE;
    }
}

JNIEXPORT jint JNICALL Java_my_proj_SoundMaker_loadSound(JNIEnv* env, jobject, jstring jFilename) {
    if (!engine) {
        __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "AudioEngine not initialized");
        return -1;
    }

    const char* filename = env->GetStringUTFChars(jFilename, nullptr);
    if (!filename) {
        __android_log_print(ANDROID_LOG_ERROR, "SoundMaker", "Failed to get filename string");
        return -1;
    }

    int32_t result = engine->loadSound(filename);
    env->ReleaseStringUTFChars(jFilename, filename);
    return result;
}

JNIEXPORT void JNICALL Java_my_proj_SoundMaker_playSound(JNIEnv*, jobject, jint soundId, jfloat volume) {
if (engine) {
engine->playSound(soundId, volume);
}
}

JNIEXPORT void JNICALL Java_my_proj_SoundMaker_stopAllSounds(JNIEnv*, jobject) {
if (engine) {
engine->stopAllSounds();
}
}

JNIEXPORT void JNICALL Java_my_proj_SoundMaker_releaseOboe(JNIEnv*, jobject) {
if (engine) {
__android_log_print(ANDROID_LOG_INFO, "SoundMaker", "Releasing AudioEngine");
engine.reset();
}
}

}