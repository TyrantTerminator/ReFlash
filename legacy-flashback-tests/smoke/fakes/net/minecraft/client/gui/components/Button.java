package net.minecraft.client.gui.components;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
public class Button {
  private int x,y,w,h; private Consumer<Button> action;
  public boolean visible=true;
  public boolean active=true;
  public void setMessage(Component c){}
  public Button(int x,int y,int w,int h,Consumer<Button> action){this.x=x;this.y=y;this.w=w;this.h=h;this.action=action;}
  public void click(){action.accept(this);}
  public void setAlpha(float alpha){}
  public void setTooltip(Tooltip tooltip){}
  public int getX(){return x;}public int getY(){return y;}public int getWidth(){return w;}public int getHeight(){return h;}
  public boolean isHoveredOrFocused(){return false;}
  public static Builder builder(Component c, Consumer<Button> fn){return new Builder(fn);}
  public static class Builder {
    private int x,y,w,h;private final Consumer<Button> fn;
    Builder(Consumer<Button> fn){this.fn=fn;}
    public Builder bounds(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;return this;}
    public Button build(){return new Button(x,y,w,h,fn);}
  }
}
