# ReFlash 0.10.0 — verification record

This report describes **checks performed locally**. They do not replace a production remapped JAR build or an actual Minecraft/VulkanMod runtime test.

## Passed offline Java/test-interface checks

- ReplayProjectTest: saved clip JSON, backup, edit operations, undo/redo.
- EditorTimelineTest: source tick/playhead mapping, selected clip bounds and split.
- VideoExportLibraryTest: selected MP4 restrictions.
- NativeRenderTest: native Momenton reflective API bridge with simulated exact v1.0.0 classes, 120 FPS settings and saved cuts.
- RecordingSaveMonitorTest: complete-file confirmation, timeout instead of false success.
- AudioLevelTest: microphone measurement math, no real audio saved in test.
- PerformanceBenchmarkTest: FPS average/minute, no frame capture.
- InteractiveBoundsTest: 741 button-layout checks at six viewport sizes.
- DiagnosticsReportTest: local privacy-reduced output.
- VulkanContractTest, VulkanClientContractTest, VulkanVisualContractTest: client entrypoint, events, bindings, navigation and source-rendered UI using fake Mojang/Fabric/Momenton classes.
- Source scanner: no direct `org.lwjgl.opengl` / Flashback integration in ReFlash Java sources; pinned Momenton nested metadata references present.

## Passed real local media tests

- Generated a short 160×90 MP4 fixture using FFmpeg; exported two selected jobs serially to 640×360 at 30 FPS.
- Separate generated fixture exported to 1280×720 at 60 FPS.
- Both operations used real FFmpeg executable; no Minecraft render data was involved.

## Pending verification

- Build with Fabric Loom through GitHub Actions for **this new version**; existing v0.9.3 build already successful, but the new build may expose actual Minecraft API/mapping differences not captured by tests.
- A running Minecraft 1.21.11 Fabric + VulkanMod profile loading v0.10 bundled Momenton, with only one copy of the dependency.
- In-world no-accidental-recording, keybind conflict handling, multiplayer block/entity updates, crash behavior and FPS impact on low-end hardware.
- Actual Momenton native renderer invoked from ReFlash Studio with saved clip cuts and FFmpeg installed.
- Microphone device sampling, prompt/OS permissions, final export audio synchronization and separate desktop audio capture (not implemented).
- Real UI appearance at different Minecraft GUI scale settings and with other installed mods.

## Claims deliberately not made

No native 4K Vulkan rendering or GPU encoder guarantee. The 4K/240 FPS option is FFmpeg resize and nominal frame-rate conversion. No independently rendered world inside the Studio editor. No Windows WASAPI loopback/meeting audio support, Shadow Rewind, full third-person camera-path editing, or verified arbitrary crash recovery.
