#!/usr/bin/env python3
"""Validate production JAR content. Does not prove runtime or export behavior."""
from pathlib import Path
from zipfile import ZipFile, BadZipFile
import json, sys, hashlib, io
VERSION = '0.9.3-vulkan-alpha'
REQUIRED = ('dev/reflash/client/ReFlashClient.class',
            'dev/reflash/client/StudioTheme.class',
            'dev/reflash/client/ReFlashPauseMenu.class',
            'dev/reflash/client/ReFlashScreen.class',
            'dev/reflash/client/ReFlashRecordingHud.class',
            'dev/reflash/core/VideoPostProcessor.class',
            'dev/reflash/core/ReplayProject.class',
            'dev/reflash/core/FfmpegCheck.class',
            'dev/reflash/client/MomentonBridge.class',
            'dev/reflash/core/VulkanSupport.class',
            'assets/reflash/textures/gui/studio_landscape.png',
            'assets/reflash/textures/gui/reflash_mark.png',
            'assets/reflash/textures/gui/reflash_button.png',
            'assets/reflash/textures/gui/reflash_mark_24.png',
            'assets/reflash/icon.png',
            'META-INF/LICENSES/Momenton-GPL-3.0-only.txt',
            'META-INF/LICENSES/ReFlash-MIT.txt')
def verify(path):
  path=Path(path)
  assert path.is_file(), f'No remapped JAR at {path}'
  with ZipFile(path) as z:
    names=set(z.namelist())
    m=json.loads(z.read('fabric.mod.json').decode('utf8'))
    assert m['id']=='reflash' and m['version']==VERSION, m
    assert m['depends'].get('minecraft')=='1.21.11',m
    assert m['depends'].get('momenton') and 'flashback' not in m['depends'],m
    nested='META-INF/jars/momenton-1.0.0.jar'
    assert nested in names, 'Missing nested Momenton JAR'
    assert any(j['file']==nested for j in m.get('jars',[])), 'Nested JAR missing from Fabric metadata'
    nested_bytes=z.read(nested)
    expected='12f11462bed41428fed8f5f9d48a335f4af6df6302262acdade68e1d58423d75'
    assert hashlib.sha256(nested_bytes).hexdigest()==expected, 'Wrong Momenton JAR SHA-256'
    with ZipFile(io.BytesIO(nested_bytes)) as dep:
      embedded=json.loads(dep.read('fabric.mod.json'))
      assert embedded['id']=='momenton', embedded
    for name in REQUIRED:
      assert name in names,f'Missing {name}'
      blob=z.read(name)
      if name.endswith('.png'):
        assert blob.startswith(bytes.fromhex('89504E470D0A1A0A'))
      elif name.endswith('.class'):
        assert blob[:4]==bytes.fromhex('CAFEBABE') and int.from_bytes(blob[6:8],'big')==65
    assert not any(x.startswith('smoke/') or x.startswith('vulkan-tests/') for x in names)
  print('PASS: Fabric metadata and classes. Minecraft runtime still untested.')
if __name__=='__main__':
  if len(sys.argv)!=2: raise SystemExit('Usage: verify_jar.py <remapped.jar>')
  try: verify(sys.argv[1])
  except (AssertionError,BadZipFile,KeyError,json.JSONDecodeError) as e: raise SystemExit(f'FAIL: {e}')
