package dev.reflash.client;
import net.minecraft.client.gui.GuiGraphics;
import com.moulberry.flashback.Flashback;
import java.nio.file.Path;

/** Software-only UI preview: visual check, NOT a real Minecraft screenshot. */
public class UiPreview {
 public static void main(String[] args)throws Exception{
   for(int tick: new int[]{60,300,650,1020}){
     Flashback.server.tick=tick;ReplayEditingBridge.addCameraKeyframe();
   }
   for(int tick: new int[]{150,850}){
     Flashback.server.tick=tick;ReplayEditingBridge.addFovKeyframe(80);
   }
   for(int tick: new int[]{50,1100}){
     Flashback.server.tick=tick;ReplayEditingBridge.addTimeKeyframe(13000);
   }
   Flashback.server.tick=600;
   var editor=new ReFlashEditorScreen(null);
   editor.width=1280;editor.height=720;editor.init();
   GuiGraphics image=new GuiGraphics(1280,720);
   if(args.length > 1 && args[1].equals("art"))
     image.previewWorld(215,48,810,449);
   editor.render(image,0,0,0);
   image.save(Path.of(args[0]));
   System.out.println("SAVED TEST PREVIEW: "+args[0]);
 }
}
