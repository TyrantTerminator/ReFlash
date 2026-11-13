package dev.reflash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Vulkan-safe integration with separately installed Momenton 1.0.0.
 * No OpenGL, framebuffer reads, or imports of Momenton implementation classes.
 * Runtime lookup isolates changes in third-party APIs and fails closed.
 * This bridge is NOT a replacement for the missing camera-path exporter.
 */
public final class MomentonBridge {
    private MomentonBridge() { }
    private static final String BASE = "com.relapzz.momenton.";

    public static boolean backendPresent() {
        try { Class.forName(BASE + "record.Recorder", false, MomentonBridge.class.getClassLoader()); return true; }
        catch (ClassNotFoundException | LinkageError ex) { return false; }
    }

    private static Object call(String name, String method, Class<?>[] types, Object... args)
            throws ReflectiveOperationException {
        try { return Class.forName(BASE + name).getMethod(method, types).invoke(null, args); }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new ReflectiveOperationException(method + " failed: " + (cause == null ? "unknown" : cause.getMessage()), cause);
        }
    }

    private static boolean booleanCall(String name, String method) {
        try { return Boolean.TRUE.equals(call(name, method, new Class<?>[0])); }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    public static boolean isRecording() { return booleanCall("record.Recorder", "isRecording"); }
    public static boolean isInReplay() { return booleanCall("playback.ReplaySession", "isPlaying"); }

    /** Native Momenton UI, not a duplicate/replacement editor. */
    public static boolean openReplayBrowser(Minecraft client, Screen parent) {
        try {
            Screen screen = (Screen) Class.forName(BASE + "ui.ReplayBrowserScreen")
                    .getConstructor(Screen.class).newInstance(parent);
            client.setScreen(screen);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            ReFlashClient.notifyPlayer("Momenton replay browser unavailable. Check Momenton 1.0.0.");
            return false;
        }
    }

    /** Uses Momenton's actual editor if a replay is running. */
    public static boolean openNativeEditor(Minecraft client) {
        if (!isInReplay()) return false;
        try {
            call("ui.editor.EditSession", "toggle", new Class<?>[]{Minecraft.class}, client);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    /** Launch actual .momenton playback. This may disconnect from a live world. */
    public static boolean openReplay(Minecraft client, Path replayFile) {
        if (!validReplay(replayFile) || isRecording()) return false;
        try {
            call("playback.ReplaySession", "open", new Class<?>[]{Minecraft.class, Path.class}, client, replayFile);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    public static boolean toggleRecording(Minecraft client) {
        if (!backendPresent() || isInReplay() || client.player == null || client.level == null) {
            ReFlashClient.notifyPlayer("Recording needs a live world and Momenton 1.0.0.");
            return false;
        }
        boolean before = isRecording();
        try {
            call("record.Recorder", "toggle", new Class<?>[]{Minecraft.class}, client);
            boolean after = isRecording();
            if (after == before) {
                ReFlashClient.notifyPlayer("Recorder did not change state. Check latest.log.");
                return false;
            }
            ReFlashClient.notifyPlayer(after ? "Replay recording started (Vulkan-safe)." : "Replay recording stopped and saving.");
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            ReFlashClient.notifyPlayer("Momenton recorder failed. Check latest.log.");
            return false;
        }
    }

    /** Local sidecar bookmarks, not a native keyframe. */
    public static boolean markHighlight(Minecraft client) {
        if (!isRecording()) {
            ReFlashClient.notifyPlayer("Start recording before adding a bookmark.");
            return false;
        }
        try {
            int tick = (int) call("record.Recorder", "elapsedTicks", new Class<?>[0]);
            Path p = dev.reflash.core.VulkanSupport.bookmarkPath(
                    net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir());
            Files.createDirectories(p.getParent());
            Files.writeString(p, System.currentTimeMillis() + "\t" + tick + "\tbookmark\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            ReFlashClient.notifyPlayer("Bookmark saved at " + (tick / 20) + "s (sidecar file).");
            return true;
        } catch (ReflectiveOperationException | IOException | LinkageError | RuntimeException ex) {
            ReFlashClient.notifyPlayer("Could not save bookmark.");
            return false;
        }
    }

    /** Opens Momenton's native export settings, without claiming unavailable Vulkan QHD rendering. */
    public static boolean openNativeExport(Minecraft client, Screen parent) {
        if (!isInReplay()) return false;
        try {
            Path file = (Path) call("playback.ReplaySession", "file", new Class<?>[0]);
            int ticks = (int) call("playback.ReplaySession", "durationTicks", new Class<?>[0]);
            if (!validReplay(file)) return false;
            Screen export = (Screen) Class.forName(BASE + "ui.ExportScreen")
                    .getConstructor(Screen.class, Path.class, int.class)
                    .newInstance(parent, file, ticks);
            client.setScreen(export);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    public static Path replayFolder(Path gameDir) { return dev.reflash.core.VulkanSupport.replayFolder(gameDir); }

    private static boolean validReplay(Path p) {
        return p != null && Files.isRegularFile(p) &&
                p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".momenton");
    }
}
