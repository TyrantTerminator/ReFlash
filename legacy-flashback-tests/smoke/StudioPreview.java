package dev.reflash.client;

import net.minecraft.client.gui.GuiGraphics;
import java.nio.file.Path;

/** Software preview of the real Java Studio rendering logic; not a live game screenshot. */
public class StudioPreview {
    public static void main(String[] args) throws Exception {
        new ReFlashClient().onInitializeClient();
        var screen = new ReFlashScreen(null);
        screen.width = 1280;
        screen.height = 720;
        if(args.length>1){
            var p=ReFlashScreen.class.getDeclaredField("page");
            p.setAccessible(true);
            @SuppressWarnings({"rawtypes", "unchecked"}) Object v=Enum.valueOf((Class<? extends Enum>)p.getType(),args[1]);
            p.set(screen,v);
        }
        screen.init();
        var image = new GuiGraphics(1280,720);
        screen.render(image,0,0,0.0f);
        image.save(Path.of(args[0]));
        System.out.println("SOURCE GUI DRAW OUTPUT: " + args[0]);
    }
}
