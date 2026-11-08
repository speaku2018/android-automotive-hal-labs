S05-P02: Build an AAOS cabin-temperature control app

Prerequisites: the property from S05-P01 is deployed and available on Android 15, with an initial value of 22.5 degrees Celsius. The Android platform checkout must already build.

Create the new project folder packages/apps/CabinTemperatureReader. Add Android.bp, AndroidManifest.xml, res/values/styles.xml and src/com/example/cabintemperature/MainActivity.java in the order shown. The starter is intentionally empty. completed/CabinTemperatureReader contains the final reference.

From the selected Android platform build shell:
m -j2 CabinTemperatureReader
adb install -r -t out/target/product/emulator_car64_x86_64/system/app/CabinTemperatureReader/CabinTemperatureReader.apk
adb shell am start -W -n com.example.cabintemperature/.MainActivity

This platform-signed teaching app uses platform APIs and android.car. It requests CAR_VENDOR_EXTENSION, a protected permission; signing and platform permission policy matter. This is not an ordinary third-party Android Studio/Gradle app.

Verify plus and minus change by 0.5 degrees. Check that confirmed values come from a read/event, rather than displaying the requested number as if it were confirmed. The callback checks availability and payload type. Reads/writes run on a worker executor; UI changes run on the UI thread. onStop unregisters the callback and disconnects; onDestroy shuts down the executor.

The emulator Extended Controls property list does not expose this teaching vendor extension. Use the native diagnostic terminal beside the main emulator display for an external update:
adb shell dumpsys android.hardware.automotive.vehicle.IVehicle/default --set 560001025 -f 24.5

The app must update from its callback without a button press. Compare native readback with CabinTemperatureReader logs, then restore 22.5.

Compatibility with later prepared exercises: this rebuild uses module CabinTemperatureReader, package com.example.cabintemperature and component com.example.cabintemperature/.MainActivity. When following prepared validation commands for CustomVehicleProperty or com.example.customvehicleproperty, substitute these new names and the APK path shown above. The vendor property contract and identifier stay the same. The original prepared app is preserved separately.
