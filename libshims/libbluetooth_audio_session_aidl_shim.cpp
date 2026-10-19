/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <dlfcn.h>
#include <stdint.h>
#include <string.h>

namespace {

// Non-trivially copyable 16-byte struct matching std::shared_ptr<BluetoothAudioSession>
// so aarch64 ABI passes its return address in x8.
struct RawSharedPtr {
    void* ptr = nullptr;
    void* ctrl = nullptr;
    RawSharedPtr() = default;
    RawSharedPtr(const RawSharedPtr& o) : ptr(o.ptr), ctrl(o.ctrl) {}
    ~RawSharedPtr() {}
};

void* GetAospLib() {
    static void* handle = dlopen("libbluetooth_audio_session_aidl.so", RTLD_NOW | RTLD_LOCAL);
    return handle;
}

void* GetAospSession(const void* mtk_self) {
    uint8_t type = *(static_cast<const uint8_t*>(mtk_self) + 0x28);
    if (type > 13 || !((1u << type) & 0x353au)) return nullptr;  // SW types: 1,3,4,5,8,10,12,13
    static void* cached[16] = {};
    if (cached[type]) return cached[type];

    using Fn = RawSharedPtr (*)(const uint8_t&);
    static auto fn = reinterpret_cast<Fn>(dlsym(
            GetAospLib(),
            "_ZN4aidl7android8hardware9bluetooth5audio29BluetoothAudioSessionInstance18GetSessionInstanceERKNS3_11SessionTypeE"));
    if (!fn) return nullptr;
    RawSharedPtr sp = fn(type);  // Intentionally retained in static cache for process lifetime
    cached[type] = sp.ptr;
    return sp.ptr;
}

}  // namespace

extern "C" {

#define FORWARD_SESSION(sym, sig, args_decl, args_mtk, args_aosp)                \
    void sym args_decl {                                                         \
        using Fn = void (*) sig;                                                 \
        static auto mtk_fn = reinterpret_cast<Fn>(dlsym(RTLD_NEXT, #sym));       \
        static auto aosp_fn = reinterpret_cast<Fn>(dlsym(GetAospLib(), #sym));   \
        if (mtk_fn) mtk_fn args_mtk;                                             \
        if (void* s = GetAospSession(self); aosp_fn && s) aosp_fn args_aosp;     \
    }

void _ZN4aidl7android8hardware9bluetooth5audio21BluetoothAudioSession16OnSessionStartedENSt3__110shared_ptrINS3_19IBluetoothAudioPortEEEPKNS1_6common3fmq12MQDescriptorIaNSA_21SynchronizedReadWriteEEERKNS3_18AudioConfigurationERKNS5_6vectorINS3_11LatencyModeENS5_9allocatorISK_EEEE(
        void* self, void* stack_iface, const void* mq_desc, const void* audio_config_v3,
        const void* latency_modes) {
    using Fn = void (*)(void*, void*, const void*, const void*, const void*);
    static auto mtk_fn = reinterpret_cast<Fn>(dlsym(RTLD_NEXT, __func__));
    static auto aosp_fn = reinterpret_cast<Fn>(dlsym(GetAospLib(), __func__));
    if (mtk_fn) mtk_fn(self, stack_iface, mq_desc, audio_config_v3, latency_modes);
    if (!audio_config_v3 || !aosp_fn) return;
    int32_t tag = *reinterpret_cast<const int32_t*>(
            static_cast<const uint8_t*>(audio_config_v3) + 0x68);
    if (tag != 0) return;  // Tag::pcmConfig
    if (void* s = GetAospSession(self)) {
        alignas(8) uint8_t config_v5[0x78] = {};
        memcpy(config_v5 + 0x08, audio_config_v3, 0x0c);
        aosp_fn(s, stack_iface, mq_desc, config_v5, latency_modes);
    }
}

void _ZN4aidl7android8hardware9bluetooth5audio21BluetoothAudioSession24ReportAudioConfigChangedERKNS3_18AudioConfigurationE(
        void* self, const void* audio_config_v3) {
    using Fn = void (*)(void*, const void*);
    static auto mtk_fn = reinterpret_cast<Fn>(dlsym(RTLD_NEXT, __func__));
    static auto aosp_fn = reinterpret_cast<Fn>(dlsym(GetAospLib(), __func__));
    if (mtk_fn) mtk_fn(self, audio_config_v3);
    if (!audio_config_v3 || !aosp_fn) return;
    int32_t tag = *reinterpret_cast<const int32_t*>(
            static_cast<const uint8_t*>(audio_config_v3) + 0x68);
    if (tag != 0) return;
    if (void* s = GetAospSession(self)) {
        alignas(8) uint8_t config_v5[0x78] = {};
        memcpy(config_v5 + 0x08, audio_config_v3, 0x0c);
        aosp_fn(s, config_v5);
    }
}

FORWARD_SESSION(
        _ZN4aidl7android8hardware9bluetooth5audio21BluetoothAudioSession14OnSessionEndedEv,
        (void*), (void* self), (self), (s))

FORWARD_SESSION(
        _ZN4aidl7android8hardware9bluetooth5audio21BluetoothAudioSession19ReportControlStatusEbNS3_20BluetoothAudioStatusE,
        (void*, bool, int32_t), (void* self, bool start_resp, int32_t status),
        (self, start_resp, status), (s, start_resp, status))

FORWARD_SESSION(
        _ZN4aidl7android8hardware9bluetooth5audio21BluetoothAudioSession34ReportLowLatencyModeAllowedChangedEb,
        (void*, bool), (void* self, bool allowed), (self, allowed), (s, allowed))

FORWARD_SESSION(
        _ZN4aidl7android8hardware9bluetooth5audio21BluetoothAudioSession14SetLatencyModeERKNS3_11LatencyModeE,
        (void*, const void*), (void* self, const void* mode), (self, mode), (s, mode))

}  // extern "C"
