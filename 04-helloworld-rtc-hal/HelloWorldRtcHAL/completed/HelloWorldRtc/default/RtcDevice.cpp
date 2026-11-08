#include "RtcDevice.h"
#include <android-base/unique_fd.h>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <linux/rtc.h>
#include <sys/ioctl.h>
#include <utility>

RtcDevice::RtcDevice(std::string path) : path_(std::move(path)) {}

int RtcDevice::readUtc(std::string* utc, std::string* error) const {
    utc->clear();
    android::base::unique_fd fd(open(path_.c_str(), O_RDONLY | O_CLOEXEC));
    if (fd.get() < 0) {
        const int code = errno;
        *error = "open " + path_ + ": " + std::strerror(code);
        return code;
    }
    rtc_time value{};
    int result;
    do { result = ioctl(fd.get(), RTC_RD_TIME, &value); }
    while (result < 0 && errno == EINTR);
    if (result < 0) {
        const int code = errno;
        *error = "RTC_RD_TIME: " + std::string(std::strerror(code));
        return code;
    }
    char text[32];
    std::snprintf(text, sizeof(text), "%04d-%02d-%02dT%02d:%02d:%02dZ",
                  value.tm_year + 1900, value.tm_mon + 1, value.tm_mday,
                  value.tm_hour, value.tm_min, value.tm_sec);
    *utc = text;
    error->clear();
    return 0;  // unique_fd closes the device on every return path.
}
