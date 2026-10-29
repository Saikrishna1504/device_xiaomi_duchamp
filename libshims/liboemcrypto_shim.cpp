/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <stdint.h>
#include <string.h>
#include <sys/auxv.h>
#include <sys/mman.h>

static int32_t (*gOrig)(void*, const char*, int32_t);

static int32_t Hooked(void* p, const char* s, int32_t n) {
    return gOrig(p, (s && n == 5 && !memcmp(s, "30770", 5)) ? "30771" : s, n);
}

__attribute__((constructor)) static void Init() {
    uintptr_t base = getauxval(AT_PHDR) & ~0xfffUL;
    if (!base || *reinterpret_cast<const uint32_t*>(base + 0x320) != 0x2686f259U) return;
    auto* slot = reinterpret_cast<decltype(&gOrig)>(base + 0x1aac88);
    uintptr_t page = reinterpret_cast<uintptr_t>(slot) & ~0xfffUL;
    if (mprotect(reinterpret_cast<void*>(page), 4096, PROT_READ | PROT_WRITE) == 0) {
        gOrig = *slot;
        *slot = Hooked;
        mprotect(reinterpret_cast<void*>(page), 4096, PROT_READ);
    }
}
