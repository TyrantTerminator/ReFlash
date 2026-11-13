#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/offline-checks
javac --release 21 -d build/offline-checks \
    $(find smoke/fakes -name '*.java') \
    src/main/java/dev/reflash/core/*.java \
    src/main/java/dev/reflash/client/ReplayEditingBridge.java \
    src/main/java/dev/reflash/client/FlashbackExportBridge.java \
    src/main/java/dev/reflash/client/StudioTheme.java \
    src/main/java/dev/reflash/client/ReFlashScreen.java \
    src/main/java/dev/reflash/client/ReFlashPauseMenu.java \
    src/main/java/dev/reflash/client/ReFlashEditorScreen.java \
    smoke/SmokeTest.java smoke/BridgeContractTest.java smoke/UiOfflineTest.java smoke/ExportContractTest.java smoke/PauseMenuContractTest.java smoke/VisualContractTest.java
java -Djava.awt.headless=true -cp build/offline-checks SmokeTest
java -Djava.awt.headless=true -cp build/offline-checks BridgeContractTest
java -Djava.awt.headless=true -cp build/offline-checks ExportContractTest
java -Djava.awt.headless=true -cp build/offline-checks dev.reflash.client.UiOfflineTest
java -Djava.awt.headless=true -cp build/offline-checks PauseMenuContractTest

java -Djava.awt.headless=true -cp build/offline-checks dev.reflash.client.VisualContractTest
