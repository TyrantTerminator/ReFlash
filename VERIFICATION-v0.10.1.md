# ReFlash 0.10.1 Verification

- All 14 offline tests (including new ReplayDurationTest and UpscaleProfileTest): PASS.
- Interactive GUI button bounds: 744 checks over 6 screen sizes: PASS.
- 2-video FFmpeg queue with SHARP quality: PASS (real FFmpeg).
- 1120x630 code-rendered UI preview: GENERATED.
- Source and edited replay duration test: 10.6s original / 7s edited / full-range restore: PASS.
- Actual Fabric Loom JAR from latest source: NOT BUILT in this runtime.
- Minecraft VulkanMod launch/in-game export: NOT TESTED.
- Actual root cause of reported seven-second recording: UNKNOWN without tester's .momenton archive / logs.
- Distinguish paused game ticks from elapsed wall time: WARNING IMPLEMENTED.
- Native 4K240 Vulkan raster output: NOT IMPLEMENTED; post-processing uses FFmpeg.
