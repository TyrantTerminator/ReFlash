# ReFlash Vulkan 0.9.2 — usability and export update

**Minecraft 1.21.11 + Fabric API + Momenton 1.0.0 + VulkanMod**

This is an alpha source update, **not an in-game-verified release**. The GitHub workflow must pass before installing the JAR.

## Changes

- Fixed the small logo's texture sampling: previously only the top-left quarter of a 32x32 image was sampled into a 16x16 button. The new asset is exactly 16x16, matching the sampling region. Also added a full 24x24 header mark.
- Moved the title-screen ReFlash access icon to the upper-left, away from the title-screen social icon; kept a small 22x22 icon on the pause menu. No giant buttons.
- Removed the misleading decorative recording-preview image from the library. ReFlash can't display real replay thumbnails yet.
- Home -> **Edit Latest** queues selected/latest `.momenton` replay playback and then opens Momenton's native clip editor. Library -> **Edit Selected** remains available.
- Added a native Fabric HUD recording indicator (`REC  mm:ss`), located top-left, top-right, or hidden. Only draws when recording in a world, not while menus are open.
- Fixed confusing FPS-test notification behavior by using distinct native toast IDs for start and stop. The F12 FPS comparison toggle also has a visible Start/End button. F8 remains record/stop, F7 Manager.
- Added **Export & Enhance** page with 720p,1080p,1440p,4K and 30/60/120/240 FPS settings. Output saved in `reflash.properties`. Native Momenton export remains the first step; post-processor uses FFmpeg to scale a saved MP4/MKV/MOV, pad while preserving aspect ratio, and produce a constant-FPS H.264 MP4 without blocking the game thread. FFmpeg must be on PATH; extra disk space and time are required.

## Critical export honesty

Under VulkanMod, **Momenton 1.0.0 forces its native render size to the current Minecraft window size**, so ReFlash **cannot produce native supersampled 4K detail** from Momenton yet. Set Momenton's native replay export to **240 FPS** at your current window resolution to render more temporal samples, then use ReFlash's post-processing stage to produce a 4K/240-FPS file. If you instead use a 20-FPS source video, FFmpeg's `fps` filter duplicates frames; it does **not** create unique smooth animation. Pixels enlarged from 720p do not become authentic 4K detail.

## Test evidence

- Offline Java compatibility, fake integration, UI test suite: passing.
- Recording badge data/corner/off tests: passing under fake Fabric GUI interfaces.
- Export profile validation and persistence (4K240): passing.
- Independent FFmpeg smoke test of 640x360/20-FPS input -> 3840x2160/240-FPS output: verified with ffprobe. This is **not** a Momenton gameplay export.
- **Not yet verified:** remapped Gradle 0.9.2 JAR, in-game UI, VulkanMod runtime and real video export.

## Install when built

Replace v0.9.1 JAR with v0.9.2, keep Momenton 1.0.0, Fabric API and VulkanMod for MC 1.21.11. Do not run both ReFlash versions.
