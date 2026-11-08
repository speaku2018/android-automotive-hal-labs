#!/usr/bin/env python3
"""Trace only the lab HAL, trigger one real Binder read, and detach cleanly."""
import os,sys,subprocess,time,re,json
from pathlib import Path
adb=os.environ.get('ADB','adb');serial=sys.argv[1]if len(sys.argv)>1 else'emulator-5556'
def shell(*args,check=True):return subprocess.run([adb,'-s',serial,'shell',*args],check=check,capture_output=True,text=True,timeout=30)
name='vendor.course.hardware.helloworldrtc-service';pids=shell('pidof',name).stdout.split();assert len(pids)==1,pids;pid=pids[0]
before=set(shell('su','0','pidof','strace',check=False).stdout.split())
proc=subprocess.Popen([adb,'-s',serial,'shell','su','0','strace','-f','-e','trace=openat,ioctl,close','-p',pid],stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
try:
 time.sleep(1.5);reply=shell('/vendor/bin/helloworld_rtc_client','read');assert 'Device: /dev/rtc0'in reply.stdout;print(reply.stdout,end='');time.sleep(.5)
finally:
 after=set(shell('su','0','pidof','strace',check=False).stdout.split());ours=after-before
 for tracer in ours:shell('su','0','kill','-INT',tracer,check=False)
 try:out,err=proc.communicate(timeout=10)
 except subprocess.TimeoutExpired:proc.terminate();out,err=proc.communicate(timeout=5)
raw=out+err;root=Path(__file__).resolve().parents[1];evidence=root/'build-evidence';evidence.mkdir(exist_ok=True);(evidence/'driver-syscalls.log').write_text(raw)
lines=[line for line in raw.splitlines()if '/dev/rtc0'in line or'RTC_RD_TIME'in line];assert any('/dev/rtc0'in line and'O_RDONLY'in line for line in lines),raw;assert any('RTC_RD_TIME'in line and re.search(r'= 0\s*$',line)for line in lines),raw
print('Observed HAL syscalls:')
for line in lines:print(line)
print('PASS: real device open and RTC_RD_TIME ioctl observed in the HAL process')
