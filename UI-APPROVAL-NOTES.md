# ReFlash v0.11 UI revision 2 — visual approval, not yet built

## Implemented in actual Java UI source

- Visible segmented choices for resolution: 720p / 1080p / 1440p / 4K
- Visible segmented choices for frame rate: 30 / 60 / 120 / 144 / 240 FPS
- Visible quality chooser: FAST / BALANCED / SHARP / ARCHIVE
- Selected-choice gradient accent and short 13-tick animated underline
- 10-tick light navigation fade and existing hover transitions
- Estimated H.264 file-size *range*, calculated from edited replay duration, resolution, FPS and compression profile
- Two clearly labeled export views: Quality & FPS, Exported Videos
- Responsive compact preset group chooser when the Minecraft GUI is small
- Original Render Replay, Upscale Selected, Queue First 6, Open Folder actions retained

## About the estimate

The range is heuristic and deliberately broad, using an illustrative bits-per-pixel calculation with a 0.5x-2.0x band around the baseline. It is not a measurement of the final output and does not predict source scene complexity, bitrate-control behavior or playback quality. Without a selected recording, the UI asks the user to choose one instead of inventing a duration.

## Important limits

- Code-rendered GUI previews are not screenshots from a live Minecraft instance.
- The standalone v0.11 replay engine remains unverified in a real Fabric/Vulkan runtime.
- Vulkan native rendering remains limited to the game framebuffer resolution; 4K video settings may rely on post-upscaling.
- FPS conversion through the current post-processor duplicates frames; it does not synthesize real motion.
- No official JAR has been built for this UI revision.
