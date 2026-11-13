package dev.reflash.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.reflash.core.Preferences;
import dev.reflash.core.PerformanceSampler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
    private static int startDelayTicks = -1;
    private static final SystemToast.SystemToastId REFLASH_TOAST = new SystemToast.SystemToastId();

    public static Preferences preferences() { return preferences; }
    public static String managerKeyName() {
        return managerBinding == null ? "F7" : managerBinding.getTranslatedKeyMessage().getString();
    }
    public static PerformanceSampler performanceSampler() { return FPS_METER; }

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
        SystemToast.add(client.getToastManager(), REFLASH_TOAST,
                Component.literal("◆ ReFlash Studio"), Component.literal(message));
    }

    @Override
    public void onInitializeClient() {
        preferences = new Preferences(FabricLoader.getInstance().getConfigDir().resolve("reflash.properties"));
        try { preferences.load(); }
        catch (IOException e) { LOGGER.warning("Could not read ReFlash settings: " + e); }

        ReFlashPauseMenu.install();

        managerBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.dashboard", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, CATEGORY));
        KeyMapping recording = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.record", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY));
        KeyMapping marker = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.marker", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, CATEGORY));
        KeyMapping benchmark = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.reflash.benchmark", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F12, CATEGORY));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            startDelayTicks = preferences.autoRecord() ? 50 : -1;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            startDelayTicks = -1;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MomentonBridge.onClientTick(client);
            while (managerBinding.consumeClick()) {
                client.setScreen(new ReFlashScreen(client.screen));
            }
            while (recording.consumeClick()) {
                MomentonBridge.toggleRecording(client);
            }
            while (marker.consumeClick()) {
                MomentonBridge.markHighlight(client);
            }
            while (benchmark.consumeClick()) {
                toggleBenchmark();
            }
            if (FPS_METER.active() && client.screen == null) FPS_METER.tick(client.getFps(), MomentonBridge.isRecording());
            if (startDelayTicks > 0) {
                startDelayTicks--;
                if (startDelayTicks == 0) {
                    startDelayTicks = -1;
                    if (client.player != null && client.level != null
                            && !MomentonBridge.isInReplay() && !MomentonBridge.isRecording()) {
                        MomentonBridge.toggleRecording(client);
                    }
                }
            }
        });
        LOGGER.info("ReFlash Vulkan add-on: records using Momenton, not realtime screen/video encoding");
    }
}
