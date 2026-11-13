package dev.reflash.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.reflash.core.Preferences;
import dev.reflash.core.PerformanceSampler;
import dev.reflash.core.RecordingSaveMonitor;
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
    private static int saveCheckTicks;
    private static boolean previousRecording;
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
    /** Momenton F9 is a SECOND recorder hotkey. Leave it unchanged until the user opts out. */
    public static String momentonRecordKey(){
        KeyMapping key=KeyMapping.get("key.momenton.toggle_recording");
        return key==null?"Unavailable":key.isUnbound()?"Unbound":key.getTranslatedKeyMessage().getString();
    }
    public static boolean unbindMomentonRecordingKey(){
        KeyMapping key=KeyMapping.get("key.momenton.toggle_recording");
        if(key==null)return false;
        key.setKey(InputConstants.UNKNOWN);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        notifyPlayer("Momenton record key unbound. Use your ReFlash recording key instead.");
        return true;
    }

    /** The only ReFlash-controlled recording action: hotkey and Manager use this same path. */
    public static boolean toggleRecording(Minecraft client) {
        boolean ok = MomentonBridge.toggleRecording(client);
        if (ok) {
            recordingCached = MomentonBridge.isRecording();
            recordingTicksCached = recordingCached ? MomentonBridge.recordingTicks() : 0;
            if(SAVE_MONITOR!=null){
                if(recordingCached){SAVE_MONITOR.clear();SAVE_MONITOR.arm();}
                else SAVE_MONITOR.begin();
            }
            previousRecording=recordingCached;
        }
        return ok;
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
        if(!java.nio.file.Files.isRegularFile(FabricLoader.getInstance().getConfigDir().resolve("momenton.json"))){
            MomentonBridge.setMicEnabled(false);
        }
        SAVE_MONITOR=new RecordingSaveMonitor(MomentonBridge.replayFolder(FabricLoader.getInstance().getGameDir()));
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
            // Momenton owns the shutdown writer; the monitor only reports when file appears.
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MomentonBridge.onClientTick(client);
            recordingCached = MomentonBridge.isRecording();
            recordingTicksCached = recordingCached ? MomentonBridge.recordingTicks() : 0;
            if(SAVE_MONITOR!=null){
                if(recordingCached&&!previousRecording){SAVE_MONITOR.clear();SAVE_MONITOR.arm();}
                if(!recordingCached&&previousRecording){SAVE_MONITOR.begin();notifyPlayer("Replay stopped; waiting for save confirmation");}
                previousRecording=recordingCached;
                if(SAVE_MONITOR.state()==RecordingSaveMonitor.State.SAVING && ++saveCheckTicks>=20){
                    saveCheckTicks=0;
                    var state=SAVE_MONITOR.check();
                    if(state==RecordingSaveMonitor.State.SAVED)notifyPlayer("Replay saved: "+SAVE_MONITOR.lastFile());
                    if(state==RecordingSaveMonitor.State.UNCONFIRMED)notifyPlayer("Save not confirmed. Inspect Momenton replays and latest.log");
                }
            }
            while (managerBinding.consumeClick()) {
                client.setScreen(new ReFlashScreen(client.screen));
            }
            while (recording.consumeClick()) {
                toggleRecording(client);
            }
            while (marker.consumeClick()) {
                MomentonBridge.markHighlight(client);
            }
            while (benchmark.consumeClick()) {
                toggleBenchmark();
            }
            if (FPS_METER.active() && client.screen == null) FPS_METER.tick(client.getFps(), MomentonBridge.isRecording());

        });
        LOGGER.info("ReFlash Vulkan add-on: records using Momenton, not realtime screen/video encoding");
    }
}
