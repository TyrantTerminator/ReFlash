# Changelog

## 0.10.0 — Quality & workflow build candidate

- Clip editing: playhead-based split, timeline click-to-seek, undo/redo, reset, non-destructive save and backup; selected replay remains explicit.
- Recording: shared user-initiated control; no automatic on-join recording; verified saved-file status after stop; compact or standard recording HUD with opacity and position controls.
- Audio: opt-in mic device check, Momenton microphone setting/device selection, independent Momenton F9 key unbind option, no implicit meeting audio capture.
- Export: optional experimental custom native Momenton export bridge with saved cuts; presets, correct video selection, serial FFmpeg queue, cancel and status, up to 4K/240 output postprocessing.
- UI: clear 'REPLAY VIEW' placeholder instead of fake preview imagery, Settings tabs, concise notifications, precise keybind and diagnostic controls.
- QA: offline tests across 6 viewport sizes (741 interactive-button placements), native exporter simulation, recording-save tests, generated-fixture FFmpeg queue and post-process tests, source metadata checks.
- Packaging: single JAR with pinned Momenton source/licensing per GPL; verify_jar checks new classes and version.

**Not implemented:** native offscreen high-resolution Vulkan frames, actual video-frame interpolation, Windows system audio loopback, Shadow Rewind ring buffer, fully embedded 3D preview, advanced camera keyframe system, complete cross-mod runtime compatibility proof.

## 0.9.3 — Custom clips and bundled Momenton

Initial cut-page, microphone configuration, custom presets, Momenton nested dependency. GitHub #6 built successfully; new v0.10 code requires its own build and launch verification.
