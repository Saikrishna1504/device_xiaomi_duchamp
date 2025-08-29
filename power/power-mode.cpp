/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <aidl/android/hardware/power/BnPower.h>
#include <android-base/file.h>
#include <android-base/logging.h>
#include <sys/ioctl.h>

#define SET_CUR_VALUE 0
#define TOUCH_DOUBLETAP_MODE 14
#define TOUCH_SUPER_REPORT 202
#define TOUCH_MAGIC 't'
#define TOUCH_IOC_SETMODE _IO(TOUCH_MAGIC, SET_CUR_VALUE)
#define TOUCH_DEV_PATH "/dev/xiaomi-touch"
#define TOUCH_ID 0

namespace aidl {
namespace google {
namespace hardware {
namespace power {
namespace impl {
namespace pixel {

using ::aidl::android::hardware::power::Mode;

bool isDeviceSpecificModeSupported(Mode type, bool* _aidl_return) {
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE:
        case Mode::GAME:
            *_aidl_return = true;
            return true;
        default:
            return false;
    }
}

bool setDeviceSpecificMode(Mode type, bool enabled) {
    int fd = open(TOUCH_DEV_PATH, O_RDWR);
    if (fd < 0) return false;

    int arg[3] = {TOUCH_ID, 0, enabled ? 1 : 0};
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE:
            arg[1] = TOUCH_DOUBLETAP_MODE;
            ioctl(fd, TOUCH_IOC_SETMODE, arg);
            break;
        case Mode::GAME:
            arg[1] = TOUCH_SUPER_REPORT;
            ioctl(fd, TOUCH_IOC_SETMODE, arg);
            break;
        default:
            close(fd);
            return false;
    }

    close(fd);
    return true;
}

}  // namespace pixel
}  // namespace impl
}  // namespace power
}  // namespace hardware
}  // namespace google
}  // namespace aidl
