package net.minecraft.client.gui.components;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
public final class EditBox {
    private String value=""; private int limit=100;
    public EditBox(Font font,int x,int y,int w,int h,Component label){}
    public void setValue(String v){value=v==null?"":v.substring(0,Math.min(v.length(),limit));}
    public String getValue(){return value;}
    public void setMaxLength(int length){limit=length;}
}
