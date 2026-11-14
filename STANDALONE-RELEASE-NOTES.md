# ReFlash v0.11.0 — one-mod engine integration (build candidate)

**Status:** source implemented; offline tests passed; single-JAR binary integration validated on a synthetic artifact; real Fabric production build and Minecraft/VulkanMod launch **not yet verified**.

## What changes

- A single Fabric mod ID, `reflash`. No dependency entry for `momenton`; no nested Momenton JAR.
- Recording lifecycle and tick processing owned by `ReFlashClient` through the internal `EmbeddedReplayRuntime`. The internal engine no longer registers its separate F9 hotkey, title-screen entry or HUD.
- New recordings are written under `<game directory>/reflash/recordings/*.reflash`.
- Exports are under `<game directory>/reflash/exports`.
- The ReFlash library, editor, save monitor and export routing use the `.reflash` extension and directories.
- A **manual**, non-destructive "Import Old Replays" button copies valid saved `.momenton` recordings from their previous folder to the new ReFlash folder, carrying over their clip sidecar projects when available. Original files are retained.
- The mod declares `momenton` as incompatible to avoid loading two copies of the same GPL-derived engine classes.

## What this does NOT mean

This is a **source fork** of Momenton v1.0.0, integrated into ReFlash under GPL-3.0-only. The underlying snapshot/action/packet encoding remains derived from that engine. ReFlash no longer requires the Momenton Fabric mod or a `.momenton` output extension, but is **not** a clean-room replay implementation.

It also does not by itself fix truncated recordings, add native Vulkan 4K render targets, or replace the actual replay viewport with a different renderer. These are separate tasks.

## Packaging design

The upstream engine JAR was compiled with Minecraft intermediary mappings. ReFlash's own Java source uses Mojang mappings. Compiling the copied upstream Yarn source using ReFlash's mappings would fail. Therefore the release build:

1. Compiles/remaps ReFlash with Fabric Loom.
2. Downloads/checksums the pinned GPL engine binary.
3. Merges its already-remapped classes and Mixin configuration into the **same top-level Fabric JAR**, excluding its separate `fabric.mod.json` and manifest.
4. Patches string constants for the new `reflash` mod namespace, recording suffix and paths. It does **not** obfuscate or alter JVM class/method descriptors.
5. Verifies one ReFlash mod ID, the engine classes, Fabric metadata, Mixins, and absence of nested dependencies.
6. Publishes corresponding GPL source and build scripts alongside the artifact.

## Verification performed

- Offline source tests: passed.
- Six-size Studio UI tests: passed.
- Legacy conversion tests: passed, including safe repeated import and sidecar continuity.
- Synthetic single-JAR merge against the original v1.0.0 engine binary: 151 classes merged; 19 string-pool changes; no second `fabric.mod.json`.
- Actual Fabric 1.21.11 remap and Vulkan launch: **not run for v0.11.0**.

**Do not replace the existing working JAR until an actual GitHub build and Minecraft launch confirm this revision.**
