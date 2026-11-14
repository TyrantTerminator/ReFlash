package dev.reflash.client;

import dev.reflash.core.UpscaleProfile;
import net.minecraft.client.gui.GuiGraphics;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.zip.*;

/** Test-only sequence of the real Java UI code's selected-preset animation. */
public class RenderPresetAnimation {
    @SuppressWarnings({"unchecked","rawtypes"})
    public static void main(String[]args)throws Exception {
        Path dir=Path.of("approval-previews/animation-frames");Files.createDirectories(dir);
        Path rp=Path.of("reflash/recordings/Animation_Sample.reflash");
        Files.createDirectories(rp.getParent());
        try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(rp))){
            z.putNextEntry(new ZipEntry("metadata.json"));
            z.write("{\"durationTicks\":1200}".getBytes());z.closeEntry();
        }
        try{
            new ReFlashClient().onInitializeClient();
            var p=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$Page");
            Method choose=ReFlashScreen.class.getDeclaredMethod("selectPage",p);choose.setAccessible(true);
            Method quality=ReFlashScreen.class.getDeclaredMethod("selectQuality",UpscaleProfile.class);quality.setAccessible(true);
            ReFlashScreen s=new ReFlashScreen(null);s.width=1120;s.height=630;s.init();
            choose.invoke(s,Enum.valueOf(p,"EDITOR"));choose.invoke(s,Enum.valueOf(p,"EXPORT"));
            for(int i=0;i<12;i++)s.tick();
            quality.invoke(s,UpscaleProfile.ARCHIVE);
            for(int i=0;i<10;i++){
                if(i>0)s.tick();
                GuiGraphics g=new GuiGraphics(1120,630);s.render(g,0,0,0);
                g.save(dir.resolve(String.format("frame_%02d.png",i)));
            }
            System.out.println("Rendered 10 selection-motion frames");
        } finally { Files.deleteIfExists(rp); }
    }
}
