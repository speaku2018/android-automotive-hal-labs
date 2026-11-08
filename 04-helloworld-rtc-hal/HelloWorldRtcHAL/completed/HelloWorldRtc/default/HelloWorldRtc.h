#pragma once
#include <aidl/vendor/course/hardware/helloworldrtc/BnHelloWorldRtc.h>
#include "RtcDevice.h"

namespace aidl::vendor::course::hardware::helloworldrtc {
class HelloWorldRtc final : public BnHelloWorldRtc {
  public:
    ndk::ScopedAStatus sayHello(const std::string& name,
                              std::string* reply) override;
    ndk::ScopedAStatus readRtc(RtcReading* reading) override;
  private:
    const RtcDevice device_;
};
}
