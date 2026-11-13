# ReFlash 0.10.0 Vulkan — BUILD CANDIDATE

**Status:** source-level and local FFmpeg tests pass; **not a production verified JAR**. Do not replace your working v0.9.3 JAR until the GitHub v0.10 workflow passes.

This release focuses on reliability and user workflows, not visually pretending to have 4K native Vulkan rendering. It is compatible in design with Momenton's state-based replays for Minecraft 1.21.11; real Vulkan launch, multiplayer capture, audio device behavior and offline export still need user testing. Prior v0.9.3 bundled-mod GitHub build passed.

## Tested features

- Triggered start/stop through same hotkey/button path, no ReFlash auto-join start.
- Recording save-state monitor verifies complete readable archive prior to 'Saved' confirmation.
- Source-rendered custom GUI: responsive timeline, clips, explicit seeking, undo/redo and non-destructive save.
- Exported MP4 selection and sequential FFmpeg postprocessing queue with controls.
- Mic OFF for new installs, custom mic device/short test, manual Momenton F9 key unbind.
- Small configurable record indicator (position, size, opacity).
- Bundled Momenton binary/attribution remains configured, with build checksum checks.

## Limitations

- '4K240' means software-upscaled target file and duplicated/interpolated-by-source frames, **not** 4K render from a 720p Vulkan window.
- Momenton handles actual replay world playback. No full interactive in-Studio world viewport or camera-path keyframe editor yet.
- The custom native renderer bridge is **experimental** until rendered in Minecraft. Export requires an installed FFmpeg executable.
- Desktop audio / meeting sound is **not** recorded. Game audio export and microphone audio are Momenton features with their own conditions.
- Crashed recording reconstruction, Shadow Rewind, native 4K framebuffer, high-end video interpolation, hardware encoders on Vulkan and full third-person tracking remain separate engine-level development projects.
- Embedded Momenton source licensing: GPL-3.0-only; see notices and source in GitHub workflow artifacts.

## GitHub release gate

1. Upload this source through `APPLY-UPDATE-AND-BUILD.bat` from the companion publisher ZIP (GitHub API integration here returns 403 for writes).
2. Confirm GitHub Actions `ReFlash Vulkan Alpha CI` green: offline tests, Fabric JAR build, SHA verification and artifact upload.
3. Extract `reflash-0.10.0-vulkan-alpha.jar` from the artifact. Do not install standalone Momenton alongside the bundled build; keep Fabric API/VulkanMod.
4. In Minecraft test F7, F8, save monitor, replay browse, trims, native export with a 10-second recording and FPS comparison. Report exact first failure; no in-game run has been performed here.
