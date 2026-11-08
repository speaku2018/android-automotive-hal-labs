#include "HelloWorldRtc.h"
#include <android-base/logging.h>

namespace aidl::vendor::course::hardware::helloworldrtc {
ndk::ScopedAStatus HelloWorldRtc::readRtc(RtcReading* reading) {
    std::string error;
    const int code = device_.readUtc(&reading->utcTime, &error);
    if (code != 0) {
        LOG(ERROR) << error;
        return ndk::ScopedAStatus::fromServiceSpecificErrorWithMessage(
                code, error.c_str());
    }
    reading->deviceNode = device_.path();
    LOG(INFO) << "RTC_RD_TIME " << reading->deviceNode
              << " -> " << reading->utcTime;
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus HelloWorldRtc::sayHello(const std::string& name,
                                        std::string* reply) {
    if (name.size() > 64) {
        return ndk::ScopedAStatus::fromExceptionCodeWithMessage(
                EX_ILLEGAL_ARGUMENT, "Name must be at most 64 UTF-8 bytes");
    }
    RtcReading reading;
    auto status = readRtc(&reading);
    if (!status.isOk()) return status;
    *reply = "Hello, " + (name.empty() ? std::string("World") : name)
           + "! RTC UTC: " + reading.utcTime
           + " | driver: " + reading.deviceNode;
    return ndk::ScopedAStatus::ok();
}
}
