# Add this new lab alongside the original HelloWorld project.
PRODUCT_PACKAGES += \
    vendor.course.hardware.helloworldrtc-service \
    helloworld_rtc_client \
    rtc_driver_probe
BOARD_SEPOLICY_DIRS += hardware/interfaces/helloworld_rtc/aidl/default/sepolicy
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += \
    hardware/interfaces/helloworld_rtc/aidl/integration/framework_matrix.xml
