package dev.reflash.client;

import dev.reflash.core.UpscaleProfile;
import net.minecraft.client.gui.GuiGraphics;
import java.nio.file.*;
import java.util.zip.*;
import java.lang.reflect.Method;

/** Test-only PNG renders using ReFlashScreen's actual Java paint commands. */
public class RenderPresetApproval {
    @SuppressWarnings({"unchecked","rawtypes"})
    public static void main(String[]args)throws Exception {
        Path outputDir=Path.of("/mnt/data/ReFlash-v0.11.0-PRESET-APPROVAL-REV2/approval-previews");
        Files.createDirectories(outputDir);
        Path replayDir=Path.of("reflash/recordings");Files.createDirectories(replayDir);
        Path replay=replayDir.resolve("Sample_Recording.reflash");
        try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(replay))){
            zip.putNextEntry(new ZipEntry("metadata.json"));
            zip.write("{\"durationTicks\":1870,\"worldName\":\"Sample Test World\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        try{
            new ReFlashClient().onInitializeClient();
            var page=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$Page");
            Method select=ReFlashScreen.class.getDeclaredMethod("selectPage",page);select.setAccessible(true);
            Method resolution=ReFlashScreen.class.getDeclaredMethod("selectResolution",int.class,int.class);resolution.setAccessible(true);
            Method rate=ReFlashScreen.class.getDeclaredMethod("selectFrameRate",int.class);rate.setAccessible(true);
            Method quality=ReFlashScreen.class.getDeclaredMethod("selectQuality",UpscaleProfile.class);quality.setAccessible(true);
            var export=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$ExportView");
            Method exportTab=ReFlashScreen.class.getDeclaredMethod("selectExportView",export);exportTab.setAccessible(true);
            for(int[] d:new int[][]{{1120,630},{854,480},{480,270}}){
                ReFlashScreen s=new ReFlashScreen(null);s.width=d[0];s.height=d[1];s.init();
                select.invoke(s,Enum.valueOf(page,"EDITOR"));select.invoke(s,Enum.valueOf(page,"EXPORT"));
                resolution.invoke(s,2560,1440);rate.invoke(s,144);quality.invoke(s,UpscaleProfile.SHARP);
                for(int i=0;i<11;i++)s.tick();
                GuiGraphics g=new GuiGraphics(d[0],d[1]);s.render(g,0,0,0);
                g.save(outputDir.resolve("EXPORT-PRESETS-"+d[0]+"x"+d[1]+".png"));
                if(d[0]==1120){
                    resolution.invoke(s,3840,2160);rate.invoke(s,240);quality.invoke(s,UpscaleProfile.ARCHIVE);
                    for(int i=0;i<10;i++)s.tick();
                    var g2=new GuiGraphics(d[0],d[1]);s.render(g2,0,0,0);
                    g2.save(outputDir.resolve("EXPORT-4K240-ARCHIVE.png"));
                    exportTab.invoke(s,Enum.valueOf(export,"VIDEOS"));
                    var g3=new GuiGraphics(d[0],d[1]);s.render(g3,0,0,0);
                    g3.save(outputDir.resolve("EXPORT-VIDEOS.png"));
                }
            }
            System.out.println("Rendered five real-code previews");
        } finally {
            Files.deleteIfExists(replay);
        }
    }
}
