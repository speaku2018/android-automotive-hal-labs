#include <aidl/vendor/course/hardware/helloworldrtc/IHelloWorldRtc.h>
#include <android/binder_manager.h>
#include <iostream>

using aidl::vendor::course::hardware::helloworldrtc::IHelloWorldRtc;
int main(int argc, char** argv) {
    const std::string name = std::string(IHelloWorldRtc::descriptor) + "/default";
    auto service = IHelloWorldRtc::fromBinder(
            ndk::SpAIBinder(AServiceManager_checkService(name.c_str())));
    if (!service) {
        std::cerr << "RTC HAL is not registered\n";
        return 2;
    }
    ndk::ScopedAStatus status;
    if (argc > 1 && std::string(argv[1]) == "read") {
        aidl::vendor::course::hardware::helloworldrtc::RtcReading value;
        status = service->readRtc(&value);
        if (status.isOk()) {
            std::cout << "RTC UTC: " << value.utcTime << "\n"
                      << "Device: " << value.deviceNode << "\n";
        }
    } else {
        std::string reply;
        status = service->sayHello(argc > 1 ? argv[1] : "Student", &reply);
        if (status.isOk()) std::cout << reply << '\n';
    }
    if (!status.isOk()) {
        std::cerr << "Binder call failed: " << status.getDescription() << '\n';
        return 1;
    }
    return 0;
}
