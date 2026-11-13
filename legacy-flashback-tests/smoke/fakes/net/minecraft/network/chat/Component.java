package net.minecraft.network.chat;
public class Component {
  public final String content;
  private Component(String v){content=v;}
  public static Component literal(String v){return new Component(v);}
  public static Component empty(){return new Component("");}
  public String getString(){return content;}
  public Component withStyle(net.minecraft.ChatFormatting c){return this;}
}
