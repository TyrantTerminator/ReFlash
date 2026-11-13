package net.minecraft.client;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
public class Minecraft {
  private static final Minecraft INSTANCE = new Minecraft();
  public Screen screen;
  public ClientLevel level=new ClientLevel();
  public Entity player=new Entity();
  public static Minecraft getInstance(){return INSTANCE;}
  public Entity getCameraEntity(){return new Entity();}
  public void setScreen(Screen screen){this.screen=screen;}
  public int getFps(){return 100;}
}
