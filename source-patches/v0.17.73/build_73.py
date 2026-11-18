#!/usr/bin/env python3
"""Rebuild ReFlash 0.17.73 Java source changes from exact 0.17.72 JAR.

This is NOT a complete GPL source-first build: the base JAR is still compiled code.
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

ROOT=Path(__file__).resolve().parent
p=argparse.ArgumentParser()
p.add_argument('--base',type=Path,required=True)
p.add_argument('--output',type=Path,required=True)
a=p.parse_args()
classes=['ReFlashAudioSource69','ReFlashAudioWarp69','ReFlashFfmpegSafety68']
with tempfile.TemporaryDirectory() as t:
    temp=Path(t)
    build=temp/'classes'
    build.mkdir()
    subprocess.run(['javac','-cp',str(a.base),'-d',str(build),*(str(ROOT/'src/dev/reflash/client'/f'{x}.java') for x in classes)],check=True)
    with ZipFile(a.base) as origin, ZipFile(a.output,'w',compression=ZIP_DEFLATED,compresslevel=6) as target:
        for entry in origin.infolist():
            file=build/entry.filename
            if entry.filename == 'fabric.mod.json':
                meta=json.loads(origin.read(entry))
                meta['version']='0.17.73'
                meta['description']='ReFlash v0.17.73 private reliability build: preserve audio on failed replay alignment and respect interrupted FFmpeg preflight.'
                data=(json.dumps(meta,indent=2,ensure_ascii=False)+'\n').encode()
            elif file.is_file():
                data=file.read_bytes()
            else:
                data=origin.read(entry)
            target.writestr(entry,data)
print('Built:',a.output)
