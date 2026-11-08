# Vehicle Speed Reader — S03-P03

Extract VehicleSpeedReader into packages/apps/ in the matching Android 15 checkout. This is a Soong platform app, not a standalone Gradle project. This attachment matches the source demonstrated in the lecture, including the background initial read and callback logging.

From the AOSP root:

```bash
source build/envsetup.sh
lunch sdk_car_md_x86_64-trunk_staging-userdebug
m -j2 VehicleSpeedReader privapp-permissions-vehiclespeedreader
adb install -r out/target/product/emulator_car64_x86_64/system/priv-app/VehicleSpeedReader/VehicleSpeedReader.apk
```

The course emulator already contains this system app and permission configuration. The install command updates that prepared app; it is not a replacement for image integration on a fresh device. Vehicle speed is obtained through CarPropertyManager with CAR_SPEED permission. Raw values are meters per second; the display multiplies them by 3.6 for kilometers per hour.
