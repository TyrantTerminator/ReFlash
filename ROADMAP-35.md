# Roadmap against 35 requested improvements (November 2026)

Statuses: **Source** = coded and checked offline; **Partial** = some functionality, significant limitations; **Pending** = not yet built. None of these are runtime-certified without a Minecraft launch.

| # | Improvement | Status / detail |
|---|---|---|
| 1 | Unified recording controller | Partial — F8 and Studio share one toggle; Momenton F9 can be manually unbound |
| 2 | Verified save notification | Source — validates new ZIP/metadata, warns on timeout |
| 3 | Recover all crashed recordings | Pending — preserves originals but cannot reconstruct incomplete state logs |
| 4 | Configurable HUD indicator | Source — corner, size, opacity, duration/saving |
| 5 | Recording sound feedback | Pending — notifications only |
| 6 | Shadow Rewind ring buffer | Pending — high-risk replay-engine work |
| 7 | Recording health monitor | Partial — elapsed timer and saved archive check, no packet-drop telemetry |
| 8 | Interactive timeline | Partial — clickable seek + clip bar, draggable edges not implemented |
| 9 | Split at playhead | Source |
| 10 | Embedded live Minecraft preview | Pending — can open Momenton replay world |
| 11 | Undo/redo | Source — bounded edit history |
| 12 | Real thumbnails | Pending — no replay-frame thumbnail extraction |
| 13 | Automatic project saving | Source — periodic dirty-project autosave |
| 14 | Camera presets and player following | Pending — backend editor needed |
| 15 | Advanced cinematic camera controls | Pending |
| 16 | Multi-track timeline/audio | Pending — only clip ranges |
| 17 | Custom end-to-end export page | Partial — native renderer is experimental, offline post-processing works |
| 18 | Automatic FFmpeg setup | Partial — detection/instructions, installation remains separate |
| 19 | Export explicit selected file | Source — selected MP4 picker and serial queue |
| 20 | Honest 4K240 labels | Source — native Vulkan limited to current framebuffer; postprocess upscales |
| 21 | Export presets | Source — resolution + FPS presets, FFmpeg CPU encode |
| 22 | Native Vulkan 4K offscreen renderer | Pending |
| 23 | True motion interpolation | Pending — no synthetic movement reconstruction |
| 24 | Export queue | Source — serial selected MP4 files, cancel, status |
| 25 | Hardware Vulkan encoding | Pending — CPU fallback only |
| 26 | Microphone test and meter | Partial — brief opt-in device test + level estimate, not continuous meter |
| 27 | Independent audio mixing tracks | Partial — Momenton supports game/mic output, custom Studio multitrack not built |
| 28 | Opt-in desktop/system-audio capture | Pending — no WASAPI loopback |
| 29 | Accurate audio sync after speed effects | Partial — Momenton native export, custom speed effects not implemented |
| 30 | First-run setup | Pending — Settings available, no wizard |
| 31 | UI consistency and polish | Partial — all ReFlash pages use theme, tested offline, live appearance unverified |
| 32 | Keybind conflict settings | Partial — Minecraft remap and explicit unbind Momenton F9, no automatic conflict scan |
| 33 | Performance benchmarks | Partial — avg/min FPS with constant memory; no I/O/packet metrics |
| 34 | Full modpack and offline/Vulkan compatibility | Pending — requires real systems testing |
| 35 | Reliable release automation | Source — GitHub Actions metadata/license/checksum verification, needs v0.10 run |

The three most demanding engine projects (native high-resolution Vulkan rendering, Shadow Rewind, and full desktop-audio capture) should be developed and validated separately to keep gameplay performance stable.
