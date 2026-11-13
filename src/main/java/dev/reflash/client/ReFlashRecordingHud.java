package dev.reflash.client;

import dev.reflash.core.Preferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Constant-time top corner recording indicator. Never encodes frames or reads GPU textures. */
public final class ReFlashRecordingHud {
    private ReFlashRecordingHud() { }
    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Preferences config = ReFlashClient.preferences();
        if (config == null || config.hudPosition() == Preferences.HudPosition.OFF ||
                client.player == null || client.level == null || client.screen != null ||
                !ReFlashClient.cachedRecording()) return;
        int t = Math.max(0, ReFlashClient.cachedRecordingTicks()) / 20;
        String label = String.format(java.util.Locale.ROOT, "REC  %02d:%02d", t/60, t%60);
        int w = Math.max(86, client.font.width(label) + 28), h = 18;
        int x = config.hudPosition() == Preferences.HudPosition.TOP_LEFT ? 7 : graphics.guiWidth()-w-7;
        int y = 7;
        graphics.fill(x, y, x+w, y+h, 0xD5081020);
        graphics.fill(x, y, x+2, y+h, 0xFFFF617D);
        graphics.fill(x+8, y+6, x+14, y+12, 0xFFFF385E);
        graphics.drawString(client.font, label, x+19, y+5, 0xFFFFFFFF, false);
    }
}
