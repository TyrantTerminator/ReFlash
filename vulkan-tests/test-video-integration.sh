#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash vulkan-tests/test.sh
if command -v ffmpeg >/dev/null 2>&1; then
  javac --release 21 -cp build/vulkan-contract -d build/vulkan-contract vulkan-tests/ExportQueueIntegrationTest.java vulkan-tests/VideoProcessorIntegrationTest.java
  java -Djava.awt.headless=true -cp build/vulkan-contract ExportQueueIntegrationTest
  java -Djava.awt.headless=true -cp build/vulkan-contract VideoProcessorIntegrationTest
fi
