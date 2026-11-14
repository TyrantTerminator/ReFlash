package dev.reflash.core;
import java.nio.file.Path;
import java.util.Locale;
/** Pure filesystem and compatibility rules. No graphics API dependencies. */
public final class VulkanSupport {
    private VulkanSupport() {}
    public static Path replayFolder(Path gameDir) { return gameDir.resolve("reflash").resolve("recordings"); }
    public static Path bookmarkPath(Path gameDir) { return gameDir.resolve("reflash").resolve("bookmarks.tsv"); }
    public static boolean isReplayFile(Path path) {
        return path != null && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".reflash");
    }
    /** Unsupported Vulkan offscreen export is never silently presented as a supported 1440p preset. */
    public static boolean canRenderResolution(int requestedWidth, int requestedHeight,
                                              int windowWidth, int windowHeight, boolean vulkan) {
        return !vulkan || (requestedWidth == windowWidth && requestedHeight == windowHeight);
    }
}
