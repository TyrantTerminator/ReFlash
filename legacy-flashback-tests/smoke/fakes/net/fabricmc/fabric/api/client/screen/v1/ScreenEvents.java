package net.fabricmc.fabric.api.client.screen.v1;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
public class ScreenEvents {
 public interface AfterInit { void afterInit(Minecraft client, Screen screen, int width, int height); }
 public static final Bus AFTER_INIT=new Bus();
 public static class Bus {
   private AfterInit callback;
   public void register(AfterInit callback) {this.callback=callback;}
   public void fire(Minecraft client, Screen screen, int width, int height){if(callback!=null)callback.afterInit(client,screen,width,height);}
 }
}
