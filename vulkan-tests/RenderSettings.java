package dev.reflash.client;
import java.nio.file.Path;
import net.minecraft.client.gui.GuiGraphics;
public class RenderSettings {
  public static void main(String[] args) throws Exception {
    new ReFlashClient().onInitializeClient();
    var screen = new ReFlashScreen(null);
    screen.width=960;screen.height=540;screen.init();
    screen.widgets.get(5).click(); // Settings navigation
    var g = new GuiGraphics(960,540);screen.render(g,0,0,0);
    g.save(Path.of("/mnt/data/ReFlash-0.9.1-SETTINGS-CODE-RENDER.png"));
    var small = new ReFlashScreen(null);
    small.width=320;small.height=240;small.init();
    small.widgets.get(5).click();
    var h = new GuiGraphics(320,240);small.render(h,0,0,0);
    h.save(Path.of("/mnt/data/ReFlash-0.9.1-SETTINGS-COMPACT-CODE-RENDER.png"));
  }
}
