#include "RtcDevice.h"
#include <iostream>

int main(int argc, char** argv) {
    RtcDevice device(argc > 1 ? argv[1] : "/dev/rtc0");
    std::string utc, error;
    const int code = device.readUtc(&utc, &error);
    if (code != 0) {
        std::cerr << "Driver error " << code << ": " << error << '\n';
        return 1;
    }
    std::cout << device.path() << " RTC_RD_TIME -> " << utc << '\n';
    return 0;
}
