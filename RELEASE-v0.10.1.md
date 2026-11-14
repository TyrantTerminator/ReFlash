ReFlash v0.10.1 Vulkan - source implementation / build candidate

INSTALLABLE JAR NOT YET BUILT.

1. Extract this ZIP completely.
2. Double-click APPLY-UPDATE-AND-BUILD.bat (Git for Windows required).
3. GitHub Actions: https://github.com/TyrantTerminator/ReFlash/actions
4. After a GREEN build, download the ReFlash-0.10.1 alpha artifact ZIP, unzip its .jar, replace old ReFlash JAR.
5. Remove standalone Momenton, since it is bundled with ReFlash; keep Fabric API and VulkanMod.

Fixes: source-vs-edited duration shown; Restore Full Recording; invalid archive checks;
4 working post-processing presets (Fast / Balanced / Sharp / Archive); visible video folder;
bright 16x16 logo; diagnostic recording game ticks vs elapsed wall time.

The user's tester video showed 0 to 10.6 seconds in Momenton's source export range.
A seven-second video does not establish which stage lost time. The new duration diagnostics
separate a truncated source replay from an edited project that shortens it.

Native Vulkan upscale beyond the window framebuffer remains unsupported. Offline FFmpeg
resizing is not AI super-resolution. Constant-FPS upconversion repeats frames.
The custom ReFlash Manager/Export pages are Java code; in-game visual quality still needs testing.

GitHub is used for the actual Fabric Loom build. No source ZIP should be renamed to .jar.
