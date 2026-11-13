package dev.reflash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import java.nio.file.Path;
import java.lang.reflect.Method;

public class Render092 {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        new ReFlashClient().onInitializeClient();
        Class<? extends Enum> pageClass=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$Page");
        Method select=ReFlashScreen.class.getDeclaredMethod("selectPage",pageClass);
        select.setAccessible(true);
        for(String page:new String[]{"CAPTURE","LIBRARY","EDITOR","EXPORT","PERFORMANCE","SETTINGS"}){
            ReFlashScreen screen=new ReFlashScreen(null);
            screen.width=1120; screen.height=630;
            screen.init();
            select.invoke(screen,Enum.valueOf(pageClass,page));
            GuiGraphics graphics=new GuiGraphics(1120,630);
            screen.render(graphics,0,0,0);
            graphics.save(Path.of("/mnt/data/reflash_review/ReFlash-0.9.2-"+page+"-JAVA.png"));
            System.out.println("Rendered "+page+" from Java UI code");
        }
    }
}
