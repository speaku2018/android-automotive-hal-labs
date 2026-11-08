#include "HelloWorld.h"

#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>

using aidl::android::hardware::helloworld::HelloWorld;

int main() {
    ABinderProcess_setThreadPoolMaxThreadCount(0);
    std::shared_ptr<HelloWorld> service = ndk::SharedRefBase::make<HelloWorld>();
    const std::string instance = std::string(HelloWorld::descriptor) + "/default";

    CHECK_EQ(STATUS_OK,
             AServiceManager_addService(service->asBinder().get(), instance.c_str()))
            << "Could not register " << instance;
    LOG(INFO) << "HelloWorld HAL registered as " << instance;

    ABinderProcess_joinThreadPool();
    return EXIT_FAILURE;
}
