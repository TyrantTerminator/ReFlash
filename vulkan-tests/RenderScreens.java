package dev.reflash.client;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
public class RenderScreens {
  public static void main(String[] args) throws Exception {
    new ReFlashClient().onInitializeClient();
    var studio=new ReFlashScreen(null);studio.width=960;studio.height=540;studio.init();
    var g=new GuiGraphics(960,540);studio.render(g,0,0,0);
    g.save(Path.of("/mnt/data/ReFlash-0.9.1-MANAGER-CODE-RENDER.png"));
    // Simulated title background; the icon itself is rendered from the registered Java callback.
    var title=new TitleScreen();
    ScreenEvents.AFTER_INIT.fire(Minecraft.getInstance(),title,960,540);
    var t=new GuiGraphics(960,540);
    var btn=title.widgets.get(0);
    StudioTheme.rounded(t,btn.getX(),btn.getY(),btn.getWidth(),btn.getHeight(),0xFF293050);
    StudioTheme.border(t,btn.getX(),btn.getY(),btn.getWidth(),btn.getHeight(),StudioTheme.PURPLE);
    ScreenEvents.afterRender(title).fire(title,t);
    t.save(Path.of("/mnt/data/ReFlash-0.9.1-BUTTON-CODE-RENDER.png"));
  }
}
