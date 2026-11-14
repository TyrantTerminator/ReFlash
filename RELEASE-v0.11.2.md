# ReFlash v0.11.2 — Editor & FPS UX correction (build candidate)

## Fixes implemented in source

1. Library **Edit Selected** stays inside ReFlash Studio rather than opening the legacy replay editor.
2. **Preview Replay** launches world playback separately; ReFlash does not yet render the world inside the editing pane.
3. The remaining engine-owned ExportScreen is redirected to custom ReFlash Export Studio, including when opened from outside the custom editor.
4. FPS presets 30/60/120/144/240 are immediately visible when the Export tab opens at compact GUI sizes (the compact screen defaults to FPS, rather than Size).
5. Desktop-like editor arrangement: preview workspace, clip inspector, time ruler, colored timeline clips, playhead and simple cut controls.
6. Improved responsive editor: Save Cuts and Split remain accessible at small GUI scales, while normal size shows trim, split and save.
7. Editor export button includes the selected FPS; high-contrast labels make unselected FPS options legible.
8. Regression tests verify those GUI entry points across multiple screen sizes.

## Verification

Offline Java integration, code-rendering and boundary checks pass. Actual Fabric remapping and Minecraft/Vulkan runtime tests have not been performed for v0.11.2. This package does NOT contain a compiled installable mod.

## Known limitations

The large editor preview remains a placeholder until a replay is opened in the engine's world view.
The native renderer under VulkanMod uses the actual window framebuffer size, not arbitrary native 4K.
FPS presets are passed to the native export settings; actual 240-FPS output must be tested.
Video post-processing cannot recreate detail or motion missing from the original frames.
