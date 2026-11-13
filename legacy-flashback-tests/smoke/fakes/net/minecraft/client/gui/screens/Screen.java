package net.minecraft.client.gui.screens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.*;
public class Screen {
  public int width=1280,height=720;
  protected Minecraft minecraft=Minecraft.getInstance();
  protected Font font=new Font();
  public final List<Button> widgets=new ArrayList<>();
  protected Screen(Component c){}
  protected void init(){}
  protected <T extends Button>T addRenderableWidget(T button){ widgets.add(button); return button; }
  protected void rebuildWidgets(){widgets.clear();init();}
  public void render(GuiGraphics g,int mx,int my,float t){}
  public void tick(){}
  public boolean isPauseScreen(){return true;}
  public void onClose(){}
}
