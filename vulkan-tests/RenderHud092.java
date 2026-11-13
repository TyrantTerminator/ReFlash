package dev.reflash.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import java.nio.file.Path;

/** Renders the actual HUD code against test interfaces; not a Minecraft screenshot. */
public class RenderHud092 {
    public static void main(String[] args)throws Exception {
        new ReFlashClient().onInitializeClient();
        Minecraft client=Minecraft.getInstance(); client.setScreen(null);
        com.relapzz.momenton.record.Recorder.running=true;
        com.relapzz.momenton.record.Recorder.elapsed=1480;
        ClientTickEvents.END_CLIENT_TICK.listener.accept(client);
        for(var position:dev.reflash.core.Preferences.HudPosition.values()){
            ReFlashClient.preferences().setHudPosition(position);
            GuiGraphics g=new GuiGraphics(480,200);
            HudElementRegistry.Holder.last.render(g,null);
            g.save(Path.of("/mnt/data/reflash_review/ReFlash-0.9.2-HUD-"+position+".png"));
        }
    }
}
