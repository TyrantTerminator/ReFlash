# ReFlash v0.11.1 — Friend-test regression fixes

## Confirmed issues from the 2026-11-14 uploaded test recording
- Header logo repeats/tiles: ReFlash drew a 37px UV region from a 24px texture. Fixed with exact-size 37px/35px assets and matching texture sizes.
- Replay Library Import/Refresh toolbar overlapped its heading. Shifted controls below heading and adjusted the list layout.
- Recording started from the main menu was actionable even though no world was loaded. Disabled Start Recording until a world is available; pressing F8 outside a world still shows a clear explanation.
- Recording save status was confusing and Library could remain stale. Home now shows Saving..., prevents starting a second take during finalization, and refreshes new recordings after the asynchronous writer finishes.
- Duplicate recording toast messages reduced; clearer status shown.

## Verification
- All offline Java integration tests including focused friend-video regressions passed.
- Checked the structure of the previous v0.11.0 integrated engine JAR. Full v0.11.1 Fabric JAR not built or launched in Minecraft here.
- No claim that 7-second truncated recordings, the original replay engine or Vulkan video export are fully fixed: tester did not supply the saved .reflash archive or latest.log. Those require an actual world recording/export test.
- This is a **source build candidate**, not a ready-to-install mod. Do not replace v0.11.0 until GitHub build passes.
