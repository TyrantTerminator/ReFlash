# ReFlash 0.9.3 Vulkan — custom cut UI and bundled engine candidate

**Not released or game-tested.** Do not replace your working 0.9.2 until the build is green and tested.

- Manual-only capture through the shared F8 and Manager button. ReFlash's old on-join auto-record preference is disabled on upgrade. Momenton F9 remains a separate recording key unless unbound.
- Custom cut editor with metadata duration, microphone-track detection, trim, split, delete, save. Saving to `.momenton.json` backs up a previous project once; **other editor state (HUD settings) may be reset by a save from ReFlash**, so keep the backup.
- Integrated microphone enable/disable and input-device cycle using Momenton config (changes apply to next recording). No system/meeting audio capture.
- FFmpeg detection in export UI and pinned 4K/240 output processing. Native Vulkan rendering remains constrained to window resolution. No motion interpolation.
- GitHub Actions uses verified upstream Momenton 1.0.0 binary within Fabric `jars` entry and includes its GPL source as artifact for proper redistribution. The user can install the resulting single ReFlash JAR with Fabric API; duplicate standalone Momenton should be removed during testing.
- Build, mixin/runtime compatibility, export, audio and all-in-one nested dependency must be tested in Minecraft before claiming release quality.
