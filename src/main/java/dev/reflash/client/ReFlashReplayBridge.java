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
 * Vulkan-safe access to the integrated, GPL-licensed replay engine.
 * No OpenGL, framebuffer reads, or imports of ReFlash engine implementation classes.
 * No additional Fabric mod is required. Uses a private internal engine API.
 * This bridge is NOT a replacement for the missing camera-path exporter.
 */
public final class ReFlashReplayBridge {
    private ReFlashReplayBridge() { }
    private static final String BASE = "com.relapzz.momenton.";
    private static int editQueueTicks;
    // Remember the Studio screen only while the user is editing a replay they opened from ReFlash.
    // ReFlash engine's own replay browser and export controls are not globally overridden.
    private static ReFlashScreen studioEditorOrigin;

    /** Select -> Edit directly from Library, even when playback needs an async world transition. */
    public static boolean openReplayForEditing(Minecraft client, Path replayFile) {
        editQueueTicks = 0;
        ReFlashScreen origin=client.screen instanceof ReFlashScreen ref?ref:null;
        if (!openReplay(client, replayFile)) return false;
        studioEditorOrigin=origin;
        editQueueTicks = 600;
        return true;
    }

    public static void onClientTick(Minecraft client) {
        if(client.screen!=null && client.screen.getClass().getName().equals(BASE+"ui.ExportScreen")){
            // Standalone ReFlash must never expose the legacy engine export form.
            // Preserve the current replay, even if its native editor initiated Export.
            Path activeReplay=null;
            try{ activeReplay=(Path)call("playback.ReplaySession","file",new Class<?>[0]); }
            catch(ReflectiveOperationException|LinkageError|RuntimeException ignored){}
            ReFlashScreen studio=studioEditorOrigin;
            studioEditorOrigin=null; editQueueTicks=0;
            if(studio!=null){client.setScreen(studio);studio.openExportTab();}
            // Never retain the legacy form as parent: Close would otherwise reopen it.
            else client.setScreen(ReFlashScreen.exporting(null,activeReplay));
            return;
        }
        if (editQueueTicks <= 0) return;
        if (isInReplay()) {
            editQueueTicks = 0;
            if (!openNativeEditor(client)) ReFlashClient.notifyPlayer("Open ReFlash and choose Editor to continue.");
        } else if (--editQueueTicks == 0) {
            ReFlashClient.notifyPlayer("Replay editor did not open. Try loading it from the library.");
        }
    }


    public static boolean backendPresent() {
        try { Class.forName(BASE + "record.Recorder", false, ReFlashReplayBridge.class.getClassLoader()); return true; }
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
    public static int recordingTicks() {
        try { return (int) call("record.Recorder", "elapsedTicks", new Class<?>[0]); }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return 0; }
    }

    public static boolean isInReplay() { return booleanCall("playback.ReplaySession", "isPlaying"); }

    /** Native ReFlash engine UI, not a duplicate/replacement editor. */
    public static boolean openReplayBrowser(Minecraft client, Screen parent) {
        try {
            Screen screen = (Screen) Class.forName(BASE + "ui.ReplayBrowserScreen")
                    .getConstructor(Screen.class).newInstance(parent);
            client.setScreen(screen);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            ReFlashClient.notifyPlayer("ReFlash replay browser unavailable. Check logs.");
            return false;
        }
    }

    /** Uses ReFlash engine's actual editor if a replay is running. */
    public static boolean openNativeEditor(Minecraft client) {
        if (!isInReplay()) return false;
        try {
            call("ui.editor.EditSession", "toggle", new Class<?>[]{Minecraft.class}, client);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    /** Launch actual .reflash playback. This may disconnect from a live world. */
    public static boolean openReplay(Minecraft client, Path replayFile) {
        if (!validReplay(replayFile) || isRecording()) return false;
        try {
            call("playback.ReplaySession", "open", new Class<?>[]{Minecraft.class, Path.class}, client, replayFile);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    public static boolean toggleRecording(Minecraft client) {
        if (!backendPresent() || isInReplay() || client.player == null || client.level == null) {
            ReFlashClient.notifyPlayer("Recording requires a live world and an initialized ReFlash engine.");
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
            // ReFlashClient owns state notifications and save confirmation.
            // Avoid duplicate/triple status toasts from the bridge, client and GUI.
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            ReFlashClient.notifyPlayer("ReFlash recorder failed. Check latest.log.");
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

    /** Opens ReFlash engine's native export settings, without claiming unavailable Vulkan QHD rendering. */
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

    /** ReFlash engine's actual microphone preference. Changes take effect at the next recording. */
    public static boolean micEnabled() {
        try {
            Object cfg = call("config.MomentonConfig", "get", new Class<?>[0]);
            return cfg.getClass().getField("micEnabled").getBoolean(cfg);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    public static boolean setMicEnabled(boolean enabled) {
        try {
            Object cfg = call("config.MomentonConfig", "get", new Class<?>[0]);
            cfg.getClass().getField("micEnabled").setBoolean(cfg, enabled);
            cfg.getClass().getMethod("save").invoke(cfg);
            ReFlashClient.notifyPlayer(enabled ? "Microphone enabled for NEXT recording" : "Microphone disabled for NEXT recording");
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    public static String micDevice() {
        try {
            Object cfg = call("config.MomentonConfig", "get", new Class<?>[0]);
            String value = (String) cfg.getClass().getField("micDevice").get(cfg);
            return value == null || value.isBlank() ? "System default" : value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return "Unavailable"; }
    }

    @SuppressWarnings("unchecked")
    public static boolean cycleMicDevice() {
        try {
            Object cfg = call("config.MomentonConfig", "get", new Class<?>[0]);
            var devices = (java.util.List<String>)call("audio.MicRecorder", "devices", new Class<?>[0]);
            var options = new java.util.ArrayList<String>(); options.add(""); options.addAll(devices);
            String current = (String)cfg.getClass().getField("micDevice").get(cfg);
            int next = (options.indexOf(current) + 1) % options.size();
            cfg.getClass().getField("micDevice").set(cfg, options.get(next));
            cfg.getClass().getMethod("save").invoke(cfg);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) { return false; }
    }

    public static Path replayFolder(Path gameDir) { return dev.reflash.core.VulkanSupport.replayFolder(gameDir); }

    private static boolean validReplay(Path p) {
        return p != null && Files.isRegularFile(p) &&
                p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".reflash");
    }
}
