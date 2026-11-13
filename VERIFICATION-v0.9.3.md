# ReFlash v0.9.3 verification report

**Tested here (offline):**
- Source compiles with the v0.9.3 offline Minecraft/Fabric/Momenton stand-ins (Java 21).
- Recording and replay bridge tests pass against simulated Momenton API classes.
- New `ReplayProjectTest` confirms metadata extraction, microphone flag, trim, split, save/reload, and backup of prior edits.
- Software-rendered ReFlash Studio pages render at six test screen sizes; gallery reflects current Java drawing code (decorative imagery not actual replay video).
- No direct OpenGL calls or Flashback dependencies in ReFlash Java source.
- v0.9.3 GitHub workflow stages Momenton 1.0.0 nested JAR, checks SHA-256, downloads GPL source, remaps/builds, verifies nested JAR metadata and uploads a build artifact. **This workflow has not run yet.**

**Not tested:** actual remapped v0.9.3 JAR compilation; Fabric Loader nested-mod resolution; VulkanMod launch in Fastclient; live microphone device selection and audio alignment; full clip playback; 4K/240 FPS native renderer; FFmpeg on Windows; direct system audio capture (not implemented). This is a build candidate, not an official release.

**Cautions:**
- Using ReFlash's cut editor to overwrite an existing Momenton `.json` project makes a `.reflash-backup`, but may drop existing Momenton HUD edits; restore the backup if needed.
- Momenton's separate F9 shortcut remains and can independently start/stop recording. ReFlash no longer auto-starts recording on world join. For only one record shortcut, unbind Momenton F9 via Minecraft Controls.
- System audio/meeting voices are **not** recorded. Microphone selection affects the *next* recording.
- FFmpeg still must be installed to export videos.
