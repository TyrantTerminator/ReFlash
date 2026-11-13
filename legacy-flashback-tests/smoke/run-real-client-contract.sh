#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/client-contract
fakefiles=$(find smoke/fakes -name '*.java' \
  ! -path '*/dev/reflash/client/ReFlashClient.java' \
  ! -path '*/dev/reflash/client/FlashbackBridge.java' \
  ! -path '*/net/minecraft/client/Minecraft.java' \
  ! -path '*/net/minecraft/resources/Identifier.java' \
  ! -path '*/net/minecraft/client/renderer/RenderPipelines.java')
javac --release 21 -d build/client-contract \
  $fakefiles $(find smoke/client-fakes -name '*.java') \
  src/main/java/dev/reflash/core/*.java \
  src/main/java/dev/reflash/client/*.java \
  smoke/ClientEntrypointContractTest.java
java -Djava.awt.headless=true -cp build/client-contract ClientEntrypointContractTest
