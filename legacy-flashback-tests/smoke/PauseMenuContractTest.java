import dev.reflash.client.ReFlashPauseMenu;
import dev.reflash.core.PauseMenuLayout;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import java.util.List;

public class PauseMenuContractTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args) {
  ReFlashPauseMenu.install();
  for(int[] size:new int[][]{{320,240},{426,240},{640,360},{854,480},{1280,720},{1920,1080}}){
   PauseScreen screen=new PauseScreen();
   ScreenEvents.AFTER_INIT.fire(Minecraft.getInstance(),screen,size[0],size[1]);
   List<Button> buttons=screen.widgets;
   var layout=PauseMenuLayout.forViewport(size[0],size[1]);
   check(buttons.size()==1,"Expected exactly one ReFlash button");
   Button b=buttons.get(0);
   check(b.getX()==layout.x() && b.getY()==layout.y(),"incorrect position");
   check(b.getWidth()==layout.width() && b.getHeight()==layout.buttonHeight(),"incorrect size");
   check(b.getX()>=0 && b.getX()+b.getWidth()<=size[0],"horizontal overflow");
   check(b.getY()>=0 && b.getY()+b.getHeight()<=size[1],"vertical overflow");
   check(b.active,"launcher should always be active");
   b.click();
   check(Minecraft.getInstance().screen instanceof dev.reflash.client.ReFlashScreen,
         "launcher must open full Studio menu even when replay loaded");
  }
  System.out.println("PASS: single ReFlash pause button at six resolutions and Studio navigation");
 }
}
