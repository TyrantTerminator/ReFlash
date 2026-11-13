#!/usr/bin/env python3
"""Validate production JAR content. Does not prove runtime or export behavior."""
from pathlib import Path
from zipfile import ZipFile, BadZipFile
import json, sys
VERSION = '0.9.1-vulkan-alpha'
REQUIRED = ('dev/reflash/client/ReFlashClient.class',
            'dev/reflash/client/StudioTheme.class',
            'dev/reflash/client/ReFlashPauseMenu.class',
            'dev/reflash/client/ReFlashScreen.class',
            'dev/reflash/client/MomentonBridge.class',
            'dev/reflash/core/VulkanSupport.class',
            'assets/reflash/textures/gui/studio_landscape.png',
            'assets/reflash/textures/gui/reflash_mark.png',
            'assets/reflash/textures/gui/reflash_button.png',
            'assets/reflash/icon.png')
def verify(path):
  path=Path(path)
  assert path.is_file(), f'No remapped JAR at {path}'
  with ZipFile(path) as z:
    names=set(z.namelist())
    m=json.loads(z.read('fabric.mod.json').decode('utf8'))
    assert m['id']=='reflash' and m['version']==VERSION, m
    assert m['depends'].get('minecraft')=='1.21.11',m
    assert m['depends'].get('momenton') and 'flashback' not in m['depends'],m
    for name in REQUIRED:
      assert name in names,f'Missing {name}'
      blob=z.read(name)
      if name.endswith('.png'):
        assert blob.startswith(bytes.fromhex('89504E470D0A1A0A'))
      else:
        assert blob[:4]==bytes.fromhex('CAFEBABE') and int.from_bytes(blob[6:8],'big')==65
    assert not any(x.startswith('smoke/') or x.startswith('vulkan-tests/') for x in names)
  print('PASS: Fabric metadata and classes. Minecraft runtime still untested.')
if __name__=='__main__':
  if len(sys.argv)!=2: raise SystemExit('Usage: verify_jar.py <remapped.jar>')
  try: verify(sys.argv[1])
  except (AssertionError,BadZipFile,KeyError,json.JSONDecodeError) as e: raise SystemExit(f'FAIL: {e}')
