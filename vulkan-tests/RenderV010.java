package dev.reflash.client;

import net.minecraft.client.gui.GuiGraphics;
import java.nio.file.Path;
import java.lang.reflect.Method;

/** Render the real Java screen's draw commands through a test GuiGraphics implementation. */
public class RenderV010 {
 @SuppressWarnings({"unchecked","rawtypes"})
 public static void main(String[]args)throws Exception {
    new ReFlashClient().onInitializeClient();
    var pageClass=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$Page");
    var select=ReFlashScreen.class.getDeclaredMethod("selectPage",pageClass);select.setAccessible(true);
    for(String page:new String[]{"CAPTURE","LIBRARY","EDITOR","EXPORT","PERFORMANCE","SETTINGS"}){
        ReFlashScreen screen=new ReFlashScreen(null);screen.width=1120;screen.height=630;screen.init();
        select.invoke(screen,Enum.valueOf(pageClass,page));
        GuiGraphics g=new GuiGraphics(1120,630);screen.render(g,0,0,0);
        g.save(Path.of("/mnt/data/ReFlash-v0.10.2-preview/"+page+".png"));
        System.out.println(page+" " + g.paintCount+" draw calls");
    }
    Class<? extends Enum> tab=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$SettingsTab");
    Method switchTab=ReFlashScreen.class.getDeclaredMethod("chooseSettings",tab);switchTab.setAccessible(true);
    ReFlashScreen screen=new ReFlashScreen(null);screen.width=1120;screen.height=630;screen.init();
    select.invoke(screen,Enum.valueOf(pageClass,"SETTINGS"));
    for(String name:new String[]{"RECORDING","AUDIO","KEYS"}){
        switchTab.invoke(screen,Enum.valueOf(tab,name));
        GuiGraphics g=new GuiGraphics(1120,630);screen.render(g,0,0,0);
        g.save(Path.of("/mnt/data/ReFlash-v0.10.2-preview/SETTINGS-"+name+".png"));
    }
 }
}
