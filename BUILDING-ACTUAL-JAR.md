# Build ReFlash 0.10.0

For the easiest route, download and extract the `ReFlash-0.10.0-UPDATE-AND-BUILD.zip` package, run `APPLY-UPDATE-AND-BUILD.bat`, and sign in to GitHub if prompted. It uploads this source to your existing `TyrantTerminator/ReFlash` repository and starts the CI build. It cannot build or patch an installed Minecraft mod without running GitHub Actions.

On GitHub, open https://github.com/TyrantTerminator/ReFlash/actions and find the newest successful `ReFlash Vulkan Alpha CI` run. Under Artifacts, download **ReFlash-0.10.0-Vulkan-BUNDLED-alpha-UNTESTED**, extract the `.jar`, remove older ReFlash and standalone Momenton from the `mods` folder, and install this JAR alongside compatible Fabric API + VulkanMod. Install FFmpeg separately to export videos.

A green GitHub build means **compilation and artifact verification**, not a proven in-game launch or working 4K Vulkan renderer. For failures, send the build run URL; compiler logs can be inspected directly through GitHub. See `VERIFICATION.md`.
