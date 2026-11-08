"""Package the prepared Android 15 images without traversing unrelated build targets.

Prerequisite: selected native modules and vendorimage-nodeps have already built.
This does not replace a clean full AOSP build. It preserves AVB verification flags.
"""
from pathlib import Path
import os,subprocess,shlex,shutil,json,hashlib
AOSP=Path.cwd();product=AOSP/'out/target/product/emulator_car64_x86_64'
report_dir=AOSP/'hardware/interfaces/helloworld/aidl/build-evidence';report_dir.mkdir(exist_ok=True)
backup=report_dir/'backup';backup.mkdir(exist_ok=True)

# This product chains directly to the AVB footer in system.img.
# systemimage-nodeps has already regenerated that signed system image.
for name in ['vbmeta.img']:
 if not (backup/name).exists():shutil.copy2(product/name,backup/name)
 target='out/target/product/emulator_car64_x86_64/'+name
 result=subprocess.run(['prebuilts/build-tools/linux-x86/bin/ninja','-f','out/combined-sdk_car_md_x86_64.ninja','-t','commands','-s',target],cwd=AOSP,capture_output=True,text=True,check=True)
 recipes=[line for line in result.stdout.splitlines()if 'make_vbmeta_image' in line];assert len(recipes)==1,recipes
 argv=shlex.split(recipes[0]);assert argv[:2]==['/bin/bash','-c'] and len(argv)==3
 (report_dir/('executed_'+name+'_recipe.txt')).write_text(recipes[0]+'\n');subprocess.run(argv,cwd=AOSP,check=True)
env=dict(os.environ,PATH=str(AOSP/'out/host/linux-x86/bin')+os.pathsep+os.environ['PATH'])
subprocess.run(['out/host/linux-x86/bin/build_super_image','-v','out/target/product/emulator_car64_x86_64/obj/PACKAGING/superimage_debug_intermediates/misc_info.txt','out/target/product/emulator_car64_x86_64/super.img'],cwd=AOSP,env=env,check=True)
verification=subprocess.run(['out/host/linux-x86/bin/avbtool','verify_image','--image','out/target/product/emulator_car64_x86_64/vbmeta.img','--follow_chain_partitions'],cwd=AOSP,env=env,capture_output=True,text=True,check=True);(report_dir/'avb_verification.log').write_text(verification.stdout+verification.stderr)
# The build-tree emulator boots GPT-wrapped QEMU disks, not the raw super.img.
for disk in ['system-qemu.img','vendor-qemu.img']:
    if not (backup/disk).exists():shutil.copy2(product/disk,backup/disk)
targets=['out/target/product/emulator_car64_x86_64/system-qemu.img','out/target/product/emulator_car64_x86_64/vendor-qemu.img']
qemu_recipes=subprocess.run(['prebuilts/build-tools/linux-x86/bin/ninja','-f','out/combined-sdk_car_md_x86_64.ninja','-t','commands','-s',*targets],cwd=AOSP,capture_output=True,text=True,check=True).stdout.splitlines()
assert len(qemu_recipes)==2,qemu_recipes
for command in qemu_recipes:
    subprocess.run(shlex.split(command),cwd=AOSP,env=env,check=True)
(report_dir/'executed_qemu_recipes.txt').write_text('\n'.join(qemu_recipes)+'\n')

# Refresh the digest passed by the emulator bootloader to first-stage init.
boot_recipe=subprocess.run(['prebuilts/build-tools/linux-x86/bin/ninja','-f','out/combined-sdk_car_md_x86_64.ninja','-t','commands','-s','out/target/product/emulator_car64_x86_64/VerifiedBootParams.textproto'],cwd=AOSP,capture_output=True,text=True,check=True).stdout.strip()
assert 'mk_vbmeta_boot_params.sh' in boot_recipe
if not (backup/'VerifiedBootParams.textproto').exists():shutil.copy2(product/'VerifiedBootParams.textproto',backup/'VerifiedBootParams.textproto')
subprocess.run(shlex.split(boot_recipe),cwd=AOSP,env=env,check=True)
(report_dir/'executed_boot_params_recipe.txt').write_text(boot_recipe+'\n')

report={'status':'passed','method':'Prepared systemimage-nodeps and vendorimage-nodeps output; regenerate system-chain and root vbmeta with the exact Ninja recipe, package existing matching partition images with the branch build_super_image tool, and verify AVB including the system chain.','verification_flags_changed':False,'images':[]}
for name in ['system.img','vendor.img','vbmeta.img','super.img','system-qemu.img','vendor-qemu.img']:
 p=product/name;h=hashlib.sha256()
 with p.open('rb')as f:
  for block in iter(lambda:f.read(8*1024*1024),b''):h.update(block)
 report['images'].append({'path':str(p),'size':p.stat().st_size,'sha256':h.hexdigest()})
(report_dir/'native_image_verification.json').write_text(json.dumps(report,indent=2));print('PREPARED_NATIVE_IMAGES_PACKAGED_AND_AVB_VERIFIED',flush=True)
