#pragma once

#include <aidl/android/hardware/helloworld/BnHelloWorld.h>

namespace aidl::android::hardware::helloworld {

    class HelloWorld final : public BnHelloWorld {
    public:
        ndk::ScopedAStatus sayHello(const std::string& name, std::string* greeting) override;
    };

}  // namespace aidl::android::hardware::helloworld
