#!/usr/bin/env python3
"""Integrate and patch the GPL-licensed Momenton v1.0.0 intermediary classes.

Builds a *single Fabric mod*. The upstream binary is compiled with intermediary
mappings, which lets it run alongside ReFlash's Loom-remapped Mojang-mapped Java.
No second fabric.mod.json, nested jar, second entrypoint or external Momenton
mod is installed. The complete corresponding GPL source is distributed separately.
"""
from __future__ import annotations
import json
import os
import struct
import sys
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

EXPECTED_SHA256='12f11462bed41428fed8f5f9d48a335f4af6df6302262acdade68e1d58423d75'

def rewrite_class_constants(raw: bytes) -> tuple[bytes,int]:
    if raw[:4]!=b'\xca\xfe\xba\xbe':raise ValueError('Not a Java class')
    total=struct.unpack_from('>H',raw,8)[0]
    n=1;off=10;changed=0;out=bytearray(raw[:10])
    while n<total:
        tag=raw[off];off+=1;out.append(tag)
        if tag==1:
            size=struct.unpack_from('>H',raw,off)[0];off+=2
            data=raw[off:off+size];off+=size
            # Patch ONLY literal pool values, never JVM class paths or method descriptors.
            if data==b'momenton':
                data=b'reflash'
            elif data==b'.momenton':
                data=b'.reflash'
            elif data.startswith(b'\x01.momenton'):
                data=data.replace(b'.momenton',b'.reflash')
            elif data==b'momenton:playback':
                data=b'reflash:playback'
            elif data==b'momenton.json':
                data=b'reflash-engine.json'
            elif data==b'momenton.ffmpeg':
                data=b'reflash.ffmpeg'
            elif data.startswith((b'momenton-video-',b'momenton-audio-',b'momenton-mic-')):
                data=b'reflash'+data[len(b'momenton'):]
            if data!=raw[off-size:off]:changed+=1
            out+=struct.pack('>H',len(data))+data
        elif tag in (3,4,9,10,11,12,17,18):
            size=4;out+=raw[off:off+size];off+=size
        elif tag in (5,6):
            size=8;out+=raw[off:off+size];off+=size;n+=1
        elif tag in (7,8,16,19,20):
            size=2;out+=raw[off:off+size];off+=size
        elif tag==15:
            size=3;out+=raw[off:off+size];off+=size
        else:raise ValueError('Unsupported constant-pool tag '+str(tag))
        n+=1
    out+=raw[off:]
    return bytes(out),changed

def merge(target:Path,engine:Path):
    import hashlib
    assert hashlib.sha256(engine.read_bytes()).hexdigest()==EXPECTED_SHA256,'Wrong GPL engine binary hash'
    with ZipFile(target) as z:
        ours={name:z.read(name) for name in z.namelist() if not name.endswith('/')}
    with ZipFile(engine) as z:
        source={name:z.read(name) for name in z.namelist() if not name.endswith('/')}
    mod=json.loads(ours['fabric.mod.json']);assert mod['id']=='reflash'
    assert 'momenton' not in mod.get('depends',{}), 'External dependency still declared'
    assert not mod.get('jars'),'Nested mod still declared'
    assert not mod.get('mixins'),'Mixin config must be added after Loom remapping'
    if 'momenton.mixins.json' not in source:raise ValueError('No engine Mixin configuration')
    mod['mixins']=['momenton.mixins.json']
    ours['fabric.mod.json']=json.dumps(mod,indent=2).encode('utf-8')
    forbidden={'fabric.mod.json','META-INF/MANIFEST.MF','META-INF/LICENSE','META-INF/LICENSE.txt'}
    patched=0;count=0
    for name,data in source.items():
        if name in forbidden or name.upper().startswith('META-INF/'):
            continue
        if name.startswith('assets/momenton/'):
            # Engine IDs are now reflash. Keep ReFlash's own logo and UI assets.
            newname='assets/reflash/'+name[len('assets/momenton/'):]
            if newname.endswith('/lang/en_us.json') and newname in ours:
                original=json.loads(ours[newname]);upstream=json.loads(data)
                original.update({k:v for k,v in upstream.items() if k not in original})
                ours[newname]=json.dumps(original,ensure_ascii=False,indent=2).encode('utf-8')
            elif newname not in ours:
                ours[newname]=data
            continue
        if name in ours:raise ValueError('Engine collides with existing mod file: '+name)
        if name.startswith('com/relapzz/momenton/') and name.endswith('.class'):
            data,n=rewrite_class_constants(data);patched+=n;count+=1
        ours[name]=data
    assert count>=100, f'Expected engine bytecode, got {count} classes'
    assert patched>0,'No runtime archive-path modifications were applied'
    assert 'META-INF/jars/momenton-1.0.0.jar' not in ours
    # Atomic replacement: only replace the production JAR after all validations pass.
    tmp=target.with_name(target.name+'.merging')
    with ZipFile(tmp,'w',compression=ZIP_DEFLATED,compresslevel=7) as z:
        for name,data in sorted(ours.items()):z.writestr(name,data)
    os.replace(tmp,target)
    print(f'PASS: integrated {count} GPL engine classes, patched {patched} internal filename/namespace literals')
    print('PASS: one Fabric mod, no separate or nested Momenton dependency')

if __name__=='__main__':
    if len(sys.argv)!=3:raise SystemExit('Usage: merge_standalone_engine.py <remapped-reflash.jar> <momenton-1.0.0.jar>')
    merge(Path(sys.argv[1]),Path(sys.argv[2]))
