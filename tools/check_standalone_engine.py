#!/usr/bin/env python3
"""Structural verification for the GPL-derived embedded ReFlash engine.

This checks JAR structure and bytecode literals; it cannot simulate a Minecraft launch.
"""
import json
import struct
import sys
from pathlib import Path
from zipfile import ZipFile


def utf8_constants(class_bytes):
    if class_bytes[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('Invalid Java class header')
    count = struct.unpack_from('>H', class_bytes, 8)[0]
    offset = 10
    index = 1
    values = []
    while index < count:
        tag = class_bytes[offset]
        offset += 1
        if tag == 1:
            size = struct.unpack_from('>H', class_bytes, offset)[0]
            offset += 2
            values.append(class_bytes[offset:offset + size])
            offset += size
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            offset += 2
        elif tag == 15:
            offset += 3
        else:
            raise ValueError(f'Unexpected constant-pool tag: {tag}')
        index += 1
    return values


def check(path):
    with ZipFile(path) as jar:
        names = set(jar.namelist())
        metadata = json.loads(jar.read('fabric.mod.json'))
        assert metadata['id'] == 'reflash', 'Incorrect mod ID'
        assert set(metadata['entrypoints']) == {'client'}, 'Unexpected mod entrypoint'
        assert metadata['entrypoints']['client'] == ['dev.reflash.client.ReFlashClient'], 'Unexpected client initializer'
        assert 'momenton' not in metadata.get('depends', {}), 'External Momenton dependency declared'
        assert metadata.get('jars') is None, 'Unexpected nested mod'
        assert metadata.get('mixins') == ['momenton.mixins.json'], 'Engine mixins missing'
        mixins = json.loads(jar.read('momenton.mixins.json'))
        assert len(mixins.get('client', [])) >= 30, 'Too few engine mixins'
        engine = [n for n in names if n.startswith('com/relapzz/momenton/') and n.endswith('.class')]
        assert len(engine) >= 140, f'Only {len(engine)} engine classes'
        assert 'assets/reflash/shaders/core/nv12.fsh' in names, 'ReFlash shader missing'
        assert 'assets/reflash/textures/gui/cursor.png' in names, 'ReFlash cursor missing'
        assert 'assets/momenton/icon.png' not in names, 'Unmigrated Momenton assets'
        recorder = utf8_constants(jar.read('com/relapzz/momenton/record/Recorder.class'))
        assert b'.reflash' in recorder or b'\x01.reflash' in recorder, 'Recorder still uses old extension'
        assert b'momenton' not in recorder, 'Recorder still writes to old folder'
        config = utf8_constants(jar.read('com/relapzz/momenton/config/MomentonConfig.class'))
        assert b'reflash-engine.json' in config, 'Engine uses old settings file'
        assert b'momenton.json' not in config, 'Engine settings remain split'
        original_mod = utf8_constants(jar.read('com/relapzz/momenton/Momenton.class'))
        assert b'reflash' in original_mod, 'Integrated engine still uses old namespace'
        assert b'momenton' not in original_mod, 'Legacy namespace still active'
        for size in (35,37):
            name = f'assets/reflash/textures/gui/reflash_mark_{size}.png'
            assert name in names, f'Corrected no-repeat logo missing: {name}'
        assert 'META-INF/jars/momenton-1.0.0.jar' not in names, 'Nested dependency remains'
        print(f'PASS: one ReFlash mod, {len(engine)} integrated engine classes, replay extension and config patched')
        print('CHECK LIMIT: Minecraft/Vulkan launch, recording, playback and video export not tested')


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise SystemExit('Usage: check_standalone_engine.py <ReFlash production JAR>')
    check(Path(sys.argv[1]))
