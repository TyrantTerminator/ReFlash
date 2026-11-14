package dev.reflash.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.reflash.core.Preferences;
import dev.reflash.core.ScreenCaptureRecorder;
import dev.reflash.core.PerformanceSampler;
import dev.reflash.core.RecordingSaveMonitor;
import dev.reflash.core.RecordingSessionDiagnostics;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.logging.Logger;

public final class ReFlashClient implements ClientModInitializer {
    private static final Logger LOGGER = Logger.getLogger("ReFlash");
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("reflash", "controls"));
    private static Preferences preferences;
    private static KeyMapping managerBinding;
    private static final PerformanceSampler FPS_METER = new PerformanceSampler();
    private static RecordingSaveMonitor SAVE_MONITOR;
    private static RecordingSessionDiagnostics SESSION_DIAGNOSTICS;
    private static int saveCheckTicks;
    private static boolean previousRecording;
    private static long recordingStartedNanos;
    private static int lastCapturedTicks;
    private static boolean recordingCached;
    private static int recordingTicksCached;
    public static boolean cachedRecording() { return recordingCached; }
    public static RecordingSaveMonitor.State saveState() {return SAVE_MONITOR==null?RecordingSaveMonitor.State.IDLE:SAVE_MONITOR.state();}
    public static int cachedRecordingTicks() { return recordingTicksCached; }


    public static Preferences preferences() { return preferences; }
    public static String managerKeyName() {
        return managerBinding == null ? "F7" : managerBinding.getTranslatedKeyMessage().getString();
    }
    public static PerformanceSampler performanceSampler() { return FPS_METER; }
    /** The integrated engine has no extra F9 recorder keybind. */
    public static String engineRecordKey(){ return "Disabled (ReFlash F8 only)"; }
    public static boolean manageEngineRecordingKey(){
        notifyPlayer("ReFlash already owns the only recording hotkey (default F8).");
        return true;
    }

    /** The only ReFlash-controlled recording action: hotkey and Manager use this same path. */
    public static boolean toggleRecording(Minecraft client) {
        // Always stop the active source, even if settings changed in the meantime.
        if(ScreenCaptureRecorder.isRecording()){
            boolean stopped=ScreenCaptureRecorder.stop();
            if(stopped)notifyPlayer("Finishing screen video in ReFlash/exports...");
            return stopped;
        }
        if(!ReFlashReplayBridge.isRecording() &&
                preferences.recordingMode()==Preferences.RecordingMode.SCREEN_VIDEO){
            boolean started=ScreenCaptureRecorder.start(FabricLoader.getInstance().getGameDir(),preferences);
            if(started)notifyPlayer("Screen video recording started. Press F8 to stop.");
            else notifyPlayer(ScreenCaptureRecorder.status());
            recordingCached=started;
            return started;
        }
        boolean wasRecording=ReFlashReplayBridge.isRecording();
        // The engine finalises replay ZIPs asynchronously; a new take must not
        // begin while the previous output is still being written.
        if(!wasRecording && SAVE_MONITOR!=null &&
                SAVE_MONITOR.state()==RecordingSaveMonitor.State.SAVING){
            notifyPlayer("Previous replay is still saving. Check Library shortly.");
            return false;
        }
        int gameTicksBeforeStop=wasRecording?ReFlashReplayBridge.recordingTicks():0;
        boolean ok = ReFlashReplayBridge.toggleRecording(client);
        if (ok) {
            recordingCached = ReFlashReplayBridge.isRecording();
            notifyPlayer(recordingCached?"Recording started":"Recording stopped — saving replay...");
            recordingTicksCached = recordingCached ? ReFlashReplayBridge.recordingTicks() : 0;
            if(recordingCached)lastCapturedTicks=recordingTicksCached;
            if(SAVE_MONITOR!=null){
                if(recordingCached){
                    if(SESSION_DIAGNOSTICS!=null)SESSION_DIAGNOSTICS.started();
                    SAVE_MONITOR.clear();SAVE_MONITOR.arm();
                    recordingStartedNanos=System.nanoTime();lastCapturedTicks=0;
                } else {
                    if(SESSION_DIAGNOSTICS!=null)SESSION_DIAGNOSTICS.stopped(gameTicksBeforeStop,true,false);
                    SAVE_MONITOR.expectTicks(gameTicksBeforeStop);
                    SAVE_MONITOR.begin();
                    warnAboutPausedWorld(gameTicksBeforeStop);
                }
            }
            previousRecording=recordingCached;
        }
        return ok;
    }

    /** Ticks stop advancing in paused singleplayer worlds; wall time does not. */
    private static void warnAboutPausedWorld(int gameTicks){
        if(recordingStartedNanos<=0)return;
        double seconds=(System.nanoTime()-recordingStartedNanos)/1e9;
        double playable=gameTicks/20.0;
        if(seconds-playable>5)notifyPlayer(String.format(java.util.Locale.ROOT,
                "Recorded %.1fs of game time over %.0fs elapsed. Paused menus are excluded.",playable,seconds));
        recordingStartedNanos=0;
    }
    /** Shared keyboard/UI FPS comparison action; no gameplay pixel capture. */
    public static void toggleBenchmark() {
        if (FPS_METER.active()) {
            FPS_METER.stop();
            var result = FPS_METER.result();
            if (result.idleSamples() > 0)
                notifyPlayer(String.format(java.util.Locale.ROOT, "Replay OFF: %.0f FPS (%d samples)", result.idleFps(), result.idleSamples()));
            if (result.recordingSamples() > 0)
                notifyPlayer(String.format(java.util.Locale.ROOT, "Replay ON: %.0f FPS (%d samples)", result.recordingFps(), result.recordingSamples()));
            if (result.comparable())
                notifyPlayer(String.format(java.util.Locale.ROOT, "Approximate FPS difference: %+.1f%%. Compare in the same scene.", result.relativeChangePercent()));
            else notifyPlayer("For a fair comparison, record 5+ seconds with replay OFF and 5+ seconds ON.");
        } else {
            FPS_METER.start();
            notifyPlayer("FPS comparison started. Try F8 OFF and ON, then F12 for results.");
        }
    }

    public static void savePreferences() {
        try { preferences.save(); }
        catch (IOException e) { LOGGER.warning("Could not save ReFlash preferences: " + e); }
    }

    /** Native toast instead of an easily missed chat message. Works in menu and world. */
    public static void notifyPlayer(String message) {
        Minecraft client = Minecraft.getInstance();
        if (preferences == null || !preferences.notifications()) return;
        SystemToast.add(client.getToastManager(), new SystemToast.SystemToastId(),
                Component.literal("◆ ReFlash Studio"), Component.literal(message));
    }

    @Override
    public void onInitializeClient() {
        preferences = new Preferences(FabricLoader.getInstance().getConfigDir().resolve("reflash.properties"));
        try { preferences.load(); }
        catch (IOException e) { LOGGER.warning("Could not read ReFlash settings: " + e); }

        // Manual recording only. Discard any auto-record setting saved by older builds.
        if (preferences.autoRecord()) { preferences.setAutoRecord(false); savePreferences(); }
        // Consent-first default for brand-new installations. Existing mic preferences remain untouched.
        if(!java.nio.file.Files.isRegularFile(FabricLoader.getInstance().getConfigDir().resolve("reflash-engine.json"))){
            ReFlashReplayBridge.setMicEnabled(false);
        }
        SAVE_MONITOR=new RecordingSaveMonitor(ReFlashReplayBridge.replayFolder(FabricLoader.getInstance().getGameDir()));
        SESSION_DIAGNOSTICS=new RecordingSessionDiagnostics(FabricLoader.getInstance().getGameDir().resolve("reflash/diagnostics/recording-events.tsv"));
        EmbeddedReplayRuntime.bootstrap();
        ReFlashPauseMenu.install();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("reflash", "recording_indicator"),
                (graphics, counter) -> ReFlashRecordingHud.render(graphics));

        managerBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.dashboard", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, CATEGORY));
        KeyMapping recording = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.record", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY));
        KeyMapping marker = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.marker", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, CATEGORY));
        KeyMapping benchmark = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.benchmark", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, CATEGORY));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Never start automatically; recording is always an explicit action.
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // The internal replay engine owns the shutdown writer; the monitor only reports when file appears.
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EmbeddedReplayRuntime.tick(client);
            ReFlashReplayBridge.onClientTick(client);
            // ReFlash-owned exports return to ReFlash-styled progress/error pages.
            NativeReplayExporter.routeExportScreen(client);
            boolean replayRecording=ReFlashReplayBridge.isRecording();
            recordingCached = replayRecording || ScreenCaptureRecorder.isRecording();
            recordingTicksCached = replayRecording ? ReFlashReplayBridge.recordingTicks()
                    : ScreenCaptureRecorder.recordingTicksApprox();
            if(replayRecording)lastCapturedTicks=recordingTicksCached;
            if(SAVE_MONITOR!=null){
                if(replayRecording&&!previousRecording){
                    if(SESSION_DIAGNOSTICS!=null)SESSION_DIAGNOSTICS.started();
                    SAVE_MONITOR.clear();SAVE_MONITOR.arm();
                    recordingStartedNanos=System.nanoTime();lastCapturedTicks=recordingTicksCached;
                }
                if(!replayRecording&&previousRecording){
                    if(SESSION_DIAGNOSTICS!=null)SESSION_DIAGNOSTICS.stopped(lastCapturedTicks,false,client.level==null || client.player==null);
                    SAVE_MONITOR.expectTicks(lastCapturedTicks);
                    SAVE_MONITOR.begin();warnAboutPausedWorld(lastCapturedTicks);
                    notifyPlayer("Replay stopped; waiting for save confirmation");
                }
                previousRecording=replayRecording;
                if(SAVE_MONITOR.state()==RecordingSaveMonitor.State.SAVING && ++saveCheckTicks>=20){
                    saveCheckTicks=0;
                    var state=SAVE_MONITOR.check();
                    if(state==RecordingSaveMonitor.State.SAVED){
                        if(SESSION_DIAGNOSTICS!=null)SESSION_DIAGNOSTICS.saveResult(SAVE_MONITOR.savedTicks(),SAVE_MONITOR.shortened()?"SHORTENED":"SAVED");
                        if(SAVE_MONITOR.shortened())notifyPlayer("Replay unexpectedly shorter than recorded ticks. Check latest.log");
                        else notifyPlayer(String.format(java.util.Locale.ROOT,"Replay saved: %.1fs of game time",SAVE_MONITOR.savedTicks()/20.0));
                    }
                    if(state==RecordingSaveMonitor.State.UNCONFIRMED){
                        if(SESSION_DIAGNOSTICS!=null)SESSION_DIAGNOSTICS.saveResult(0,"UNCONFIRMED");
                        notifyPlayer("Save unconfirmed. See reflash/diagnostics/recording-events.tsv");
                    }
                }
            }
            while (managerBinding.consumeClick()) {
                client.setScreen(new ReFlashScreen(client.screen));
            }
            while (recording.consumeClick()) {
                toggleRecording(client);
            }
            while (marker.consumeClick()) {
                ReFlashReplayBridge.markHighlight(client);
            }
            while (benchmark.consumeClick()) {
                toggleBenchmark();
            }
            if (FPS_METER.active() && client.screen == null) FPS_METER.tick(client.getFps(), ReFlashReplayBridge.isRecording());

        });
        LOGGER.info("ReFlash: optional Windows desktop video (FFmpeg); replay mode stays integrated/Vulkan-friendly");
    }
}
