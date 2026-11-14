package dev.reflash.client;
import net.minecraft.client.gui.GuiGraphics;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.zip.*;
public class RenderEditorUX {
 @SuppressWarnings({"unchecked","rawtypes"})
 public static void main(String[] args)throws Exception{
    Path dir=Path.of("/mnt/data/ReFlash-v0.11.2-PREVIEWS");Files.createDirectories(dir);
    Path folder=Path.of("reflash/recordings");Files.createDirectories(folder);
    Path file=folder.resolve("Test_Replay_Parkour.reflash");
    try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(file))){z.putNextEntry(new ZipEntry("metadata.json"));z.write("{\"durationTicks\":1520,\"worldName\":\"Practice Server\"}".getBytes());z.closeEntry();}
    try{
      new ReFlashClient().onInitializeClient();
      Class cls=Class.forName("dev.reflash.client.ReFlashScreen$Page");
      Method select=ReFlashScreen.class.getDeclaredMethod("selectPage",cls);select.setAccessible(true);
      for(int[] d:new int[][]{{1120,630},{854,480},{480,270}}){
       var screen=new ReFlashScreen(null);screen.width=d[0];screen.height=d[1];screen.init();
       select.invoke(screen,Enum.valueOf(cls,"LIBRARY"));
       select.invoke(screen,Enum.valueOf(cls,"EDITOR"));
       var g=new GuiGraphics(d[0],d[1]);screen.render(g,0,0,0);
       g.save(dir.resolve("EDITOR-"+d[0]+"x"+d[1]+".png"));
       select.invoke(screen,Enum.valueOf(cls,"EXPORT"));
       var gg=new GuiGraphics(d[0],d[1]);screen.render(gg,0,0,0);
       gg.save(dir.resolve("EXPORT-"+d[0]+"x"+d[1]+".png"));
      }
      System.out.println("Rendered ReFlash v0.11.2 Editor and Export source-code UI previews");
    }finally{Files.deleteIfExists(file);}
 }
}
