#pragma once
#include <string>

class RtcDevice {
  public:
    explicit RtcDevice(std::string path = "/dev/rtc0");
    // Returns zero on success, or a positive errno on failure.
    int readUtc(std::string* utc, std::string* error) const;
    const std::string& path() const { return path_; }
  private:
    const std::string path_;
};
