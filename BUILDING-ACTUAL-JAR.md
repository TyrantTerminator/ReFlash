# How to generate the ReFlash Vulkan JAR

The source project has not been compiled against the Minecraft 1.21.11 production libraries in the assistant's environment; that environment has neither Gradle/Minecraft caches nor access to the download sites. A Java `.class` compiled against test fakes **is not a valid Minecraft release**. 

On a **Windows x64 PC with an internet connection**, double-click `BUILD-JAR-WINDOWS.bat` inside this extracted folder. The PowerShell script downloads official Gradle and Temurin JDK 21 only if needed, verifies published SHA-256 checksums, runs the full Loom build, checks the Fabric metadata and expected classes, and creates `ReFlash-Vulkan-0.9.0-ALPHA.jar`. This is a **build procedure**, not an already produced JAR. It does not install anything into Minecraft automatically.

The build depends on the following Fabric mods installed separately for runtime testing: **Fabric Loader**, **Fabric API**, **Momenton 1.0.0** and **VulkanMod** on Minecraft Java **1.21.11**. Do not install Flashback alongside this alpha; `fabric.mod.json` explicitly breaks with it.

The alpha is an interface/shortcut add-on to Momenton. Its recording and replay browser are delegated to Momenton; it is **not yet a standalone recorder** and 1440p 120 FPS export under Vulkan is not implemented in this alpha.

If the script reports failure, send `reflash-build.log`. A successful build is needed before any in-game test. Ensure your software security policies allow PowerShell, and inspect the scripts first if desired.
