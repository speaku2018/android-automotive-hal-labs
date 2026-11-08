S06 - P01 - Build a standalone HelloWorld AIDL HAL service

Start in a configured Android 15 AOSP checkout. Place the new project under
hardware/interfaces/helloworld/aidl. The starter is intentionally empty; create
the files in the order shown in the video. The completed directory is a reference.
Do not overwrite a different existing implementation without preserving it first.

Apply completed/product-inclusion.txt in the selected automotive product and merge
completed/compatibility-matrix-entry.xml into the corresponding framework matrix.
This course uses sdk_car_md_x86_64-trunk_staging-userdebug and matrix version 8.
The optional teaching HAL entry belongs to this lab's development product, not an
instruction to add arbitrary vendor services to a production framework matrix.

Build from the AOSP root:
source build/envsetup.sh
lunch sdk_car_md_x86_64-trunk_staging-userdebug
m -j2 android.hardware.helloworld-update-api
m -j2 android.hardware.helloworld-V1-ndk android.hardware.helloworld-service.example helloworld_client selinux_policy_nonsystem framework_compatibility_matrix.8.xml
m -j2 systemimage-nodeps vendorimage-nodeps

The interface is deliberately unfrozen for platform development. Follow your
branch's API freeze and compatibility procedures before a production release.
The shell client SELinux permission is limited to userdebug/eng builds.

The recorded lab uses an existing prepared build tree. The optional tools helper
regenerates the exact generated system-chain/root AVB recipes, super image,
QEMU disk wrappers, and emulator boot metadata for that target. Stop the emulator
before running it from the AOSP root. It preserves AVB settings and userdata.
For a clean checkout, use the normal full product build and matching emulator.

Runtime:
adb root
adb shell getenforce
adb shell service check android.hardware.helloworld.IHelloWorld/default
adb shell /vendor/bin/helloworld_client Student

Expected reply: Hello, Student! The new HelloWorld AIDL HAL is running.
This is a native Binder teaching service, not a physical-device driver.
