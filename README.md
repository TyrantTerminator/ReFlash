# ReFlash v0.9.3 — Vulkan-first bundled build candidate

This is an **unverified** source release. Production build and in-game VulkanMod behavior must be tested before release.

## Changes

- Manual recording only: ReFlash no longer starts capturing on world join. ReFlash F8 and the Studio record button call the same backend toggle. Note: Momenton's own F9 hotkey remains a separate action unless unbound in Minecraft controls.
- Custom ReFlash clip page: choose a .momenton replay, trim start/end by seconds, split, delete clips, save the .momenton.json project. Preview opens real replay playback and Momenton's editor. Existing project files get one `.reflash-backup` copy on first save.
- Settings: microphone toggle and JavaSound device cycling through Momenton's real configuration; these affect the next recording. Detect microphone track through replay metadata; no recording of Windows-wide audio.
- Custom Export & Enhance page: choose target resolution/FPS presets; check FFmpeg availability, render at Vulkan window size with Momenton, then upscale/duplicate frames up to 3840×2160/240 FPS. These are **output format** settings, not native 4K scene quality or interpolated motion.
- Bundled Momenton: Fabric JAR-in-JAR metadata and pinned 1.0.0 binary downloaded with SHA-256 verification during the GitHub build. One ReFlash JAR should meet the Momenton dependency after verification. Remove standalone Momenton before testing to avoid version duplication. Fabric API remains required.

## Audio and privacy

Momenton records Minecraft sound as replay-state audio to reproduce when exporting. It can capture an optional microphone WAV track via JavaSound, depending on available hardware and device selection. It does **not** capture Zoom/Teams/Discord/Windows output. Recording third-party calls should be explicitly enabled only when everyone involved knows and agrees.

FFmpeg is not included in the mod. Install FFmpeg separately or put `ffmpeg.exe` under `<game folder>/ffmpeg/bin`; ReFlash checks these paths.

## Licensing

Bundled Momenton and adapted ReplayMod components are GPL-3.0-only. Combined redistribution is subject to GPL-3.0. See THIRD_PARTY_NOTICES.md and included GPL source/license files. Original ReFlash-only source originated under MIT; its license notice remains included.
