#!/usr/bin/env python3
"""Check real driver data, error paths and scoped service restart."""
import os,sys,subprocess,time,datetime,json,re
from pathlib import Path
adb=os.environ.get('ADB','adb');serial=sys.argv[1]if len(sys.argv)>1 else'emulator-5556';client='/vendor/bin/helloworld_rtc_client';reports=[]
def command(args,expected=0,contains=None):
 p=subprocess.run([adb,'-s',serial,'shell',*args],capture_output=True,text=True,timeout=30);assert p.returncode==expected,(args,p.returncode,p.stdout,p.stderr)
 if contains:assert contains in p.stdout+p.stderr,(contains,p.stdout,p.stderr)
 reports.append({'command':args,'expected_exit':expected,'actual_exit':p.returncode,'stdout':p.stdout,'stderr':p.stderr});return p.stdout
assert command(['getenforce']).strip()=='Enforcing';print('PASS: SELinux enforcing')
ps=command(['ps','-AZ']);row=next(line for line in ps.splitlines()if'helloworldrtc-service'in line);assert 'u:r:hal_helloworldrtc_default:s0'in row and re.search(r'\bsystem\b',row);print('PASS: service uses dedicated domain and system UID')
command([client,'Course'],contains='Hello, Course! RTC UTC:');command([client+' ""'],contains='Hello, World! RTC UTC:');print('PASS: named and empty-name greetings use driver data')
def read_time():
 text=command([client,'read'],contains='Device: /dev/rtc0');stamp=re.search(r'RTC UTC: (\S+)',text)[1];return datetime.datetime.strptime(stamp,'%Y-%m-%dT%H:%M:%SZ')
a=read_time();time.sleep(2.1);b=read_time();assert 1<=(b-a).total_seconds()<=8
rtc=command(['su','0','toybox','hwclock','-r','-u','-f','/dev/rtc0']);c=datetime.datetime.strptime(rtc.strip(),'%Y-%m-%d %H:%M:%S%z').replace(tzinfo=None);assert abs((c-b).total_seconds())<=5;print('PASS: HAL clock advances and agrees with independent driver read')
command(['su','0','/vendor/bin/rtc_driver_probe','/dev/rtc-missing'],1,'No such file or directory');print('PASS: backend reports missing device')
command([client,'X'*65],1,'Name must be at most 64');print('PASS: invalid name returns Binder argument error')
try:
 command(['su','0','stop','vendor.helloworldrtc-default']);time.sleep(1);command([client,'Course'],2,'RTC HAL is not registered');print('PASS: stopped service causes bounded discovery failure')
finally:command(['su','0','start','vendor.helloworldrtc-default']);time.sleep(1)
command([client,'Restored'],contains='Hello, Restored! RTC UTC:');assert command(['getenforce']).strip()=='Enforcing';print('PASS: service restored with SELinux still enforcing')
root=Path(__file__).resolve().parents[1];out=root/'build-evidence';out.mkdir(exist_ok=True);(out/'runtime-validation.json').write_text(json.dumps({'status':'passed','serial':serial,'checks':reports},indent=2))
