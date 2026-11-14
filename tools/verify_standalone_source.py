#!/usr/bin/env python3
"""Static release gate: checks ReFlash source does not declare an external recorder mod."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
mod=json.loads((root/'src/main/resources/fabric.mod.json').read_text())
assert mod['id']=='reflash'
assert 'momenton' not in mod.get('depends',{})
assert not mod.get('jars')
assert mod.get('breaks',{}).get('momenton')=='*'
assert 'EmbeddedReplayRuntime' in (root/'src/main/java/dev/reflash/client/ReFlashClient.java').read_text()
assert '.reflash' in (root/'src/main/java/dev/reflash/core/VulkanSupport.java').read_text()
assert (root/'tools/merge_standalone_engine.py').is_file()
assert (root/'src/main/java/dev/reflash/core/LegacyReplayImporter.java').is_file()
print('PASS: one standalone ReFlash mod in source, legacy importer and integrated engine build step')
