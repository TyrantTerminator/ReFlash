package dev.reflash.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import com.moulberry.flashback.Flashback;

public class UiOfflineTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        for(int[] size : new int[][] {{320,240},{426,240},{640,360},{854,480},{1280,720},{1920,1080}}){
            var studio = new ReFlashScreen(null);
            studio.width = size[0]; studio.height = size[1];
            studio.init();
            check(!studio.widgets.isEmpty(),"no studio buttons at "+size[0]);
            for(Button b : studio.widgets){
                check(b.getX()>=0 && b.getY()>=0 && b.getX()+b.getWidth()<=size[0]+1 && b.getY()+b.getHeight()<=size[1]+1,
                     "Studio widget bounds at "+size[0]+"x"+size[1]);
            }
            GuiGraphics graphics = new GuiGraphics();
            studio.render(graphics,0,0,0.0f);
            check(graphics.paintCount>10,"studio did not paint");
            var editor = new ReFlashEditorScreen(null);
            editor.width = size[0]; editor.height = size[1];
            editor.init();
            check(!editor.widgets.isEmpty(),"no editor controls");
            for(Button b : editor.widgets){
                check(b.getX()>=0 && b.getY()>=0 && b.getX()+b.getWidth()<=size[0]+1 && b.getY()+b.getHeight()<=size[1]+1,
                     "Editor widget bounds at "+size[0]+"x"+size[1]);
            }
            GuiGraphics editorGraphics=new GuiGraphics();
            editor.render(editorGraphics,20,20,0f);
            check(editorGraphics.paintCount>25,"editor did not paint");
        }
        var editor = new ReFlashEditorScreen(null);
        editor.width=1280;editor.height=720;editor.init();
        // Find the camera keyframe button by stable inspector position.
        boolean clicked=false;
        for(Button b:editor.widgets){
            if(b.getY()==82){b.click();clicked=true;break;}
        }
        check(clicked && ReplayEditingBridge.snapshot().camera().size()==1,"camera keyframe button not wired");
        var s=ReplayEditingBridge.snapshot();
        check(s.active(),"replay state missing");
        Flashback.server=null;
        editor.tick();
        editor.render(new GuiGraphics(),0,0,0f);
        System.out.println("PASS: offline Studio/editor UI geometry at 6 screen sizes; click-to-keyframe; missing-replay rendering");
    }
}
