# ReFlash Studio 0.9.0-VULKAN-ALPHA (source only)

**This is not yet a remapped Fabric `.jar`. This is a Vulkan-first source revision, not an official tested release.**

Minecraft Java 1.21.11 / Java 21 / Fabric Loader >=0.19.3 / Fabric API / VulkanMod 0.6.8 / Momenton 1.0.0.

ReFlash 0.8.0 depended on Flashback. Flashback's renderer/exporter makes OpenGL calls that can crash under VulkanMod on Minecraft 1.21.11. This Vulkan variant **does not depend on Flashback**, and will refuse an installation with Flashback present. It launches Momenton's native recorder, replay browser and native editor via reflective method calls. Neither engine nor copyrighted third-party source is packaged into ReFlash.

## Working in source / intended behavior

- Exactly one `ReFlash` button on the Minecraft pause screen, opening the purple/cyan Studio menu.
- F7 Studio, F8 recording toggle, F6 local bookmarks, F12 optional FPS sampling. Avoid Momenton's F9 and F10 keys.
- On world join, optional auto-record through `Momenton.Recorder.toggle`.
- Recording library reads `.momenton` archives. Select a replay to play; switch into Momenton's real editor from Studio.
- Local timestamps stored at `reflash/bookmarks.tsv` (not integrated keyframes).
- No direct OpenGL calls in ReFlash source. All graphical Studio draws use Minecraft's GUI (`GuiGraphics`).

## Missing / not promised

- Freecam, keyframe camera paths, third-person replay export are **not present** in this Vulkan integration. They will need Vulkan-compatible renderer work.
- Under VulkanMod 0.6.8, Momenton 1.0.0 documents export restricted to window resolution with hardware encoders disabled. **2560x1440 at 120 FPS is NOT VERIFIED and likely unavailable at normal laptop window size.** An unverified 1440p preset cannot override this Vulkan limit.
- Actual integration cannot be verified with Minecraft here because Fabric/Minecraft dependencies and Gradle are unavailable and cannot be downloaded, and ReFlash GitHub writes receive HTTP 403. Do not present an offline Java test as a full game test.

## Installable Vulkan replay engine available for separate testing

Momenton 1.0.0: https://github.com/Relapzzz/momenton/releases/tag/v1.0.0

The pre-release offers an actual installable `momenton-1.0.0.jar`. This is **not a ReFlash JAR**, does not have the ReFlash cinematic UI, and remains its original authors' GPL-3.0 code. You can use it independently for a VulkanMod smoke test.

## Build once a networked Fabric toolchain is available

Run `gradle clean build` with Java 21. A successful Fabric Loom build creates a remapped JAR at `build/libs/`. Never use `jar cf` on unremapped Mojang-named classes as an installable substitute. Run `bash vulkan-tests/test.sh` for offline bridge contracts before the build. Additional in-game testing is mandatory.

## Windows one-click local JAR build

Double-click `BUILD-JAR-WINDOWS.bat` (internet required). See `BUILDING-ACTUAL-JAR.md` for details. This attempts a full real Fabric Loom build and only outputs a JAR if that build and JAR verification succeed. This ZIP does not include a prebuilt JAR.
