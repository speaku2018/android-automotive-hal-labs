package vendor.course.hardware.helloworldrtc;

import vendor.course.hardware.helloworldrtc.RtcReading;

@VintfStability
interface IHelloWorldRtc {
    String sayHello(String name);
    RtcReading readRtc();
}
