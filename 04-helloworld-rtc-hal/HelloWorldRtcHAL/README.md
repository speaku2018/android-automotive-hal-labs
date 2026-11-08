# HelloWorld RTC HAL — independent driver-backed extension

The original HelloWorld lab is retained under original/ and in its existing location.
The new project uses a separate AIDL package and service instance. It reads the
emulator's existing rtc_cmos driver through /dev/rtc0 and RTC_RD_TIME. The device
is virtualized by the emulator; no physical peripheral or custom kernel driver
is claimed. This lab uses a native client, as the original HelloWorld practical did.
It does not expose the vendor HAL directly to an ordinary Android APK.

Copy completed/HelloWorldRtc to hardware/interfaces/helloworld_rtc/aidl in the
selected Android 15 AOSP checkout. Add this line to the selected car vendor
product makefile, keeping the original HelloWorld inclusion:

    $(call inherit-product, hardware/interfaces/helloworld_rtc/aidl/integration/product.mk)

Select sdk_car_md_x86_64-trunk_staging-userdebug. The included AIDL API is frozen
at version 1. Build the new binaries and SELinux policy in the prepared checkout. Existing kernel and RTC permissions are retained. The HAL runs as system,
not root, in a dedicated SELinux domain. Only the RTC read ioctl is granted.

Commands after installation:

    adb -s emulator-5556 shell /vendor/bin/helloworld_rtc_client Course
    adb -s emulator-5556 shell /vendor/bin/helloworld_rtc_client read
    adb -s emulator-5556 shell su 0 /vendor/bin/rtc_driver_probe /dev/rtc0
    adb -s emulator-5556 shell su 0 /vendor/bin/rtc_driver_probe /dev/rtc-missing

The last command must return a nonzero status and an ENOENT driver error.
A working Binder greeting contains actual device time; failures do not substitute
framework wall-clock time. Runtime verification passed on the isolated Android 15 emulator with SELinux
enforcing. The native HAL runs as system in hal_helloworldrtc_default; syscall
tracing confirms a read-only open of /dev/rtc0 and a successful RTC_RD_TIME ioctl.

To validate a booted installation, place the Android SDK platform-tools on PATH
and run:

    python3 tools/validate.py emulator-5556
    bash tools/trace_driver.sh emulator-5556

The driver-name inspection is administrative on this image:

    adb -s emulator-5556 shell su 0 cat /sys/class/rtc/rtc0/name

Build the included frozen version-1 interface and native components:

    source build/envsetup.sh
    lunch sdk_car_md_x86_64-trunk_staging-userdebug
    m vendor.course.hardware.helloworldrtc-service helloworld_rtc_client rtc_driver_probe selinux_policy

For a deliberate future contract change, update and freeze the API as a separate
interface-versioning step. The included version-1 snapshot is already frozen. The build requires a prepared
AOSP checkout and the product integration above. Boot an image containing the
new vendor binaries, init/VINTF declarations, and SELinux policy; pushing only
the service executable does not install the required policy. Keep a separate
emulator data directory when retaining an existing lab emulator.

The lesson captures an incremental build; the initial AOSP build is a separate
prerequisite. Source archives exclude runtime images and private voice assets.
