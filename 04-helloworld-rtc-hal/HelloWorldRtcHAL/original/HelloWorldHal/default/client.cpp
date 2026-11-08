#include <aidl/android/hardware/helloworld/IHelloWorld.h>
#include <android/binder_manager.h>
#include <iostream>

using aidl::android::hardware::helloworld::IHelloWorld;

int main(int argc, char** argv) {
    const std::string instance = std::string(IHelloWorld::descriptor) + "/default";
    ndk::SpAIBinder binder(AServiceManager_checkService(instance.c_str()));
    std::shared_ptr<IHelloWorld> service = IHelloWorld::fromBinder(binder);
    if (service == nullptr) {
        std::cerr << "HelloWorld service is not registered\n";
        return EXIT_FAILURE;
    }

    std::string reply;
    const std::string name = argc > 1 ? argv[1] : "Student";
    ndk::ScopedAStatus status = service->sayHello(name, &reply);
    if (!status.isOk()) {
        std::cerr << "Binder call failed: " << status.getDescription() << '\n';
        return EXIT_FAILURE;
    }
    std::cout << reply << '\n';
    return EXIT_SUCCESS;
}
