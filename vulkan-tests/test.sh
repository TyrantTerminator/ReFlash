#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/vulkan-contract
legacy=legacy-flashback-tests/smoke
fakefiles=$(find "$legacy/fakes" -name '*.java' \
  ! -path '*/dev/reflash/client/ReFlashClient.java' \
  ! -path '*/dev/reflash/client/FlashbackBridge.java' \
  ! -path '*/net/minecraft/client/Minecraft.java' \
  ! -path '*/net/minecraft/resources/Identifier.java' \
  ! -path '*/net/minecraft/client/renderer/RenderPipelines.java')
clientfakefiles=$(find "$legacy/client-fakes" -name '*.java' \
  ! -path '*/org/lwjgl/glfw/GLFW.java')
javac --release 21 -d build/vulkan-contract \
  $fakefiles $clientfakefiles \
  $(find vulkan-tests/fakes -name '*.java') \
  $(find src/main/java -name '*.java') \
  vulkan-tests/VulkanContractTest.java vulkan-tests/VulkanClientContractTest.java vulkan-tests/VulkanVisualContractTest.java
java -Djava.awt.headless=true -cp build/vulkan-contract VulkanContractTest
java -Djava.awt.headless=true -cp build/vulkan-contract VulkanClientContractTest
java -Djava.awt.headless=true -cp build/vulkan-contract dev.reflash.client.VulkanVisualContractTest
if grep -R 'org.lwjgl.opengl\|com.moulberry.flashback' src/main/java; then
  echo 'FAIL: Direct OpenGL or Flashback backend reference found'; exit 1
fi
python3 - <<'PY'
import json
m=json.load(open('src/main/resources/fabric.mod.json'))
assert 'flashback' not in m['depends'] and 'momenton' in m['depends']
print('PASS: Vulkan metadata excludes Flashback and requires Momenton')
PY
