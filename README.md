# ReFlash 0.9.1 — Compact Access / Vulkan Alpha

Minecraft **1.21.11** • Fabric • Java 21 runtime • **Momenton 1.0.0 required** • VulkanMod-friendly addon.

## What changed
- One **22×22** ReFlash icon near the top-right of the title and pause menus, with a tooltip; no permanent in-game HUD overlay.
- Official ReFlash mod icon displayed in the Mods list; smaller optimized button icon and the full logo in ReFlash Manager.
- Open Manager by clicking either icon or using the registered **F7** keybinding (fully remappable through Minecraft Controls).
- Manager → Settings → **Change Keybinds** opens the actual Minecraft 1.21.11 keybind editor.
- Home shows recording, library and editor navigation immediately. Library includes **Edit Selected** for direct replay editing.
- Recordings and rendering use Momenton 1.0.0. Flashback is NOT supported in this Vulkan build. No claim of Vulkan 1440p120 or native ReFlash camera keyframes.

## Installation
Install the compiled `reflash-0.9.1-vulkan-alpha.jar`, **momenton-1.0.0.jar**, Fabric API and VulkanMod for Minecraft 1.21.11, all within the same Fabric installation. Remove the previous `reflash-0.9.0` JAR, and do not add Flashback.

## Build and verify
GitHub Actions compiles the mod using **Java 25 for Fabric Loom 1.18.2** and **Gradle 9.7.0**, with `options.release = 21` for Java 21-compatible output. Workflow attaches the remapped JAR after offline tests and metadata verification. This source package has **not** been built against real Minecraft dependencies in this environment.

## Usage
- **Title:** tiny logo at upper right → ReFlash Manager
- **Pause:** tiny logo at upper right → ReFlash Manager
- **F7:** Manager (remappable)
- **F8:** Start / stop recording (remappable)
- **F6:** Add bookmark (remappable)
- **F12:** FPS comparison (remappable)
- **Manager → Library:** choose recording → **Edit Selected** (or Open Replay)
- **Manager → Settings → Change Keybinds:** native Minecraft Controls. Find **ReFlash Controls** and change any binding.

### Honest status
The UI and integration logic have passed *simulated* Java contract tests only. The actual Minecraft launch, third-party mod interactions, replay recording, and Vulkan performance must still be tested in game. Exporting at 1440p120 on Vulkan is not promised by this alpha.
