package net.fabricmc.fabric.api.client.screen.v1;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import java.util.WeakHashMap;
public class ScreenEvents {
 public interface AfterInit { void afterInit(Minecraft client, Screen screen, int width, int height); }
 public interface AfterRender { void afterRender(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float delta); }
 public static final Bus AFTER_INIT=new Bus();
 private static final WeakHashMap<Screen,RenderBus> EVENTS=new WeakHashMap<>();
 public static RenderBus afterRender(Screen screen) { return EVENTS.computeIfAbsent(screen,s ->new RenderBus()); }
 public static class RenderBus {
   public AfterRender callback;
   public void register(AfterRender cb){callback=cb;}
   public void fire(Screen s, GuiGraphics g){if(callback!=null)callback.afterRender(s,g,0,0,0);}
 }
 public static class Bus {
   private AfterInit callback;
   public void register(AfterInit cb){callback=cb;}
   public void fire(Minecraft client, Screen screen, int w, int h){if(callback!=null)callback.afterInit(client,screen,w,h);}
 }
}
