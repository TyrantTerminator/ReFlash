# ReFlash v0.10.2 — Export UI polish candidate

Focus: consistency, typography, subtle UI movement, no new recording features.

## Changed
- The existing Export Studio remains the only settings/preset page when initiated through ReFlash.
- Export selected from Momenton's replay editor (when the replay was launched from ReFlash) returns to ReFlash's Export Studio, preserving the selected replay and cuts.
- When Momenton shows its finishing or export-error page for a ReFlash-started job, the client redirects to a ReFlash-styled status page. Exports started independently through Momenton are not touched.
- Added a custom finishing view with progress, Cancel and Open Videos, and an error view with Back to ReFlash.
- Smoothed hovered controls through lightweight, CPU-side color easing and a 13-tick navigation accent reveal. These decorations run only while ReFlash UI is visible.
- Corrected an overlapping text label in Export Studio.

## What stays the same
- No new recording engine, recording hotkeys or FPS capture changes.
- Momenton still performs in-world replay rendering and audio muxing. Its render overlay can still be visible during video rendering, and errors outside ReFlash-owned exports use Momenton's own UI.
- VulkanMod native output is still limited by the frame buffer. ReFlash upscaling is offline post-processing, not new 4K render detail.

## Verification
- `bash vulkan-tests/test.sh`: all local Java tests passed, including new simulated tests for native editor export handoff, custom finishing/error routing and explicit cancellation.
- Source-rendered mock-environment screenshots are included, not genuine Minecraft screenshots.
- No genuine Minecraft/Fabric v0.10.2 remapped JAR was built locally. GitHub Actions must compile and verify this candidate.
