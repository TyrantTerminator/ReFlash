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
                (!ReFlashClient.cachedRecording() && ReFlashClient.saveState()!=dev.reflash.core.RecordingSaveMonitor.State.SAVING)) return;
        int t = Math.max(0, ReFlashClient.cachedRecordingTicks()) / 20;
        boolean saving=!ReFlashClient.cachedRecording();
        String label = saving ? "SAVING REPLAY" : String.format(java.util.Locale.ROOT, "REC  %02d:%02d", t/60, t%60);
        boolean compact=config.hudSize()==Preferences.HudSize.COMPACT;
        int w = Math.max(compact?77:95, client.font.width(label) + (compact?24:30)), h = compact?16:20;
        int alpha=Math.clamp(config.hudOpacity()*255/100,25,255);
        int x = config.hudPosition() == Preferences.HudPosition.TOP_LEFT ? 7 : graphics.guiWidth()-w-7;
        int y = 7;
        int accent=saving?0xFFFFC15A:0xFFFF617D;
        graphics.fill(x, y, x+w, y+h, (alpha<<24)|0x00081020);
        graphics.fill(x, y, x+2, y+h, accent);
        int centerY=y+h/2;
        graphics.fill(x+7, centerY-3, x+13, centerY+3, saving?0xFFFFC15A:0xFFFF385E);
        graphics.drawString(client.font,label,x+(compact?17:20),y+(h-9)/2,0xFFFFFFFF,false);
    }
}
