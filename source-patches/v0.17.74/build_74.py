#!/usr/bin/env python3
"""ReFlash v0.17.74, reproducible patching from the v0.17.73 base JAR."""
import argparse, subprocess, json, tempfile
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED, ZipInfo

ROOT=Path(__file__).resolve().parent
ap=argparse.ArgumentParser()
ap.add_argument('--base',type=Path,default=ROOT/'ReFlash-v0.17.73.jar')
ap.add_argument('--output',type=Path,default=ROOT/'ReFlash-v0.17.74.jar')
a=ap.parse_args()
opts=['--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
      '--add-exports','java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
names=[
'dev/reflash/core/Preferences', 'dev/reflash/core/ExportSizeEstimate',
'dev/reflash/core/ReFlashMotionFilter', 'dev/reflash/core/VideoPostProcessor',
'dev/reflash/client/NativeReplayExporter', 'dev/reflash/client/ReFlashCustomOutputScreen',
'dev/reflash/client/ReFlashVideoExportScreen', 'dev/reflash/client/ReFlashTextEntryScreen', 'dev/reflash/client/ReFlashScreen',
'dev/reflash/client/ReFlashExportPanel60', 'dev/reflash/client/ReFlashAboutUI60']
with tempfile.TemporaryDirectory() as temp:
  tmp=Path(temp);classes=tmp/'classes';classes.mkdir()
  subprocess.run(['javac','-cp',str(a.base),'-d',str(classes),*(str(x) for x in sorted((ROOT/'src/dev/reflash/client').glob('ReFlash*74.java')))],check=True)
  subprocess.run(['javac',*opts,'-d',str(classes),str(ROOT/'patcher/Apply74.java')],check=True)
  with ZipFile(a.base) as zin:
    for name in names:
      orig=tmp/(name+'.original');orig.parent.mkdir(parents=True,exist_ok=True)
      orig.write_bytes(zin.read(name+'.class'))
      patched=tmp/(name+'.class');
      subprocess.run(['java',*opts,'-cp',str(classes),'Apply74',str(orig),str(patched)],check=True)
    a.output.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(a.output,'w',compression=ZIP_DEFLATED,compresslevel=6) as zout:
      seen=set()
      for info in zin.infolist():
        path=info.filename;seen.add(path)
        patched=tmp/path
        if patched.is_file():data=patched.read_bytes()
        elif path=='fabric.mod.json':
          meta=json.loads(zin.read(path));meta['version']='0.17.74'
          meta['description']='ReFlash v0.17.74 BETA: configurable 1-240 FPS replay exports, dismissible release notice, compact-screen overlap protection.'
          data=(json.dumps(meta,indent=2,ensure_ascii=False)+'\n').encode('utf-8')
        else: data=zin.read(path)
        zout.writestr(info,data)
      for file in (classes/'dev/reflash/client').glob('ReFlash*74.class'):
        name=str(file.relative_to(classes));assert name not in seen
        meta=ZipInfo(name,(2026,11,18,12,0,0))
        meta.compress_type=ZIP_DEFLATED
        zout.writestr(meta,file.read_bytes())
print('Built',a.output,'bytes',a.output.stat().st_size)
