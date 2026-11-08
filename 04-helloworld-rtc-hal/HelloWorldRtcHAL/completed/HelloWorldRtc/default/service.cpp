#include "HelloWorldRtc.h"
#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>

using aidl::vendor::course::hardware::helloworldrtc::HelloWorldRtc;
int main() {
    ABinderProcess_setThreadPoolMaxThreadCount(0);
    auto service = ndk::SharedRefBase::make<HelloWorldRtc>();
    const std::string name = std::string(HelloWorldRtc::descriptor) + "/default";
    CHECK_EQ(STATUS_OK,
             AServiceManager_addService(service->asBinder().get(), name.c_str()));
    LOG(INFO) << "HelloWorld RTC HAL registered: " << name;
    ABinderProcess_joinThreadPool();
    return EXIT_FAILURE;
}
