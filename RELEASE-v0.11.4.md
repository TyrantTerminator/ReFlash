# ReFlash v0.11.4 — Recording first (source candidate)

**Status:** source patch, not a compiled or play-tested Minecraft JAR.

## Direct recording
- Optional Windows desktop screen recording to MKV, directly in `reflash/exports/` using FFmpeg gdigrab.
- F8 toggles recording; F7 opens the ReFlash manager. Screen video may start outside a loaded world.
- Toggle microphone and desktop audio separately. The audio scanner enumerates DirectShow recording inputs. Desktop sound requires a loopback **capture source** (Stereo Mix or virtual audio device); a headset output alone is not usable as an input.
- 30 FPS default; high FPS and CPU encoding may reduce gameplay performance.

## Library / export
- Branded menu button, live status, direct My Videos access, explicit video selection.
- Open selected video, rename recordings/videos, Save Custom Copy (optional FFmpeg encoding), open exports folder.
- Custom output size / FPS / quality for saved copy; native Vulkan replay render still uses the current framebuffer dimensions.
- Previous confusing Upscale action is no longer the primary video action. No image detail generation is claimed.

## Testing
- Pure Java device-parser, FFmpeg argument construction, and safe rename tests: PASS.
- Full Fabric/Gradle compile: NOT RUN.
- Minecraft client runtime: NOT RUN.
- Microphone/headset/system audio hardware: NOT TESTED.
