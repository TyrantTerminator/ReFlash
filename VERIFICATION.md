**v0.9.2 local pass:** all offline Java tests pass; 4K240 FFmpeg synthetic output checked; no real Minecraft 1.21.11 launch or GitHub Gradle build of 0.9.2 yet. The earlier v0.9.1 build does not verify these changes.

# Vulkan verification status

- VulkanMod 0.6.8 for MC 1.21.11: published and available; Vulkan 1.2+ GPU required.
- Flashback 0.39.x VulkanMod incompatibility: publicly reported for MC 1.21.11. Removed the hard dependency in this variant.
- Momenton 1.0.0: published JAR and source, reports VulkanMod gameplay recording, replay, and export tests. Its own developer notes acknowledge Vulkan export and GPU issues.
- No direct `org.lwjgl.opengl` imports in ReFlash v0.9 Java source.
- ReFlash UI and reflected Momenton API source compilation: offline fake-interface test required (does not prove runtime).
- Genuine Fabric remapped JAR: NOT BUILT. Minecraft launch: NOT TESTED.
- 1440p120 Vulkan replay export: NOT WORKING / NOT VERIFIED.
