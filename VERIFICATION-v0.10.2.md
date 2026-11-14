# ReFlash v0.10.2 verification

Local source checked: `bash vulkan-tests/test.sh` (Java 21): PASS.

New contract: `vulkan-tests/ExportSkinRoutingTest.java` validates ReFlash-owned routes to custom Export, Finishing and Failure screens using offline fake client and Momenton interfaces. Cancelling remains an explicit user action.

Source-rendered previews generated through `RenderV010` and `RenderExportPolish` test harnesses. They are not in-game images and do not verify Minecraft drawing, compatibility or Vulkan rendering.

Production build: NOT RUN locally. GitHub can build using `.github/workflows/build.yml` (Java 21 test compilation, Java 25 Loom compilation, Gradle 9.7). Real VulkanMod launch/export: PENDING.

Still not replaced: the world view while Momenton renders video; that engine-level drawing must be considered separately. The new skin replaces finishing/error pages after video frames have rendered, not the actual renderer overlay.
