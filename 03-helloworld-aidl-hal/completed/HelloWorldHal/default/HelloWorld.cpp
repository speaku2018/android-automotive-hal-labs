#include "HelloWorld.h"

#include <android-base/logging.h>

namespace aidl::android::hardware::helloworld {

    ndk::ScopedAStatus HelloWorld::sayHello(const std::string& name, std::string* greeting) {
        const std::string who = name.empty() ? "World" : name;
        *greeting = "Hello, " + who + "! The new HelloWorld AIDL HAL is running.";
        LOG(INFO) << "sayHello called for " << who;
        return ndk::ScopedAStatus::ok();
    }

}  // namespace aidl::android::hardware::helloworld
