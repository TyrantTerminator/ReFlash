package dev.reflash.client;
import dev.reflash.client.ReFlashScreen;
import net.minecraft.client.gui.GuiGraphics;
public class VulkanVisualContractTest {
  public static void main(String[] args){
    for(int[] dim:new int[][]{{1280,720},{960,540},{854,480},{640,360},{480,270},{320,240}}){
      ReFlashScreen s =new ReFlashScreen(null);s.width=dim[0];s.height=dim[1];s.init();
      GuiGraphics g=new GuiGraphics(dim[0],dim[1]);s.render(g,0,0,0);
      if(g.paintCount<25)throw new AssertionError("No GUI drawn at "+dim[0]+"x"+dim[1]);
    }
    System.out.println("PASS: Studio UI drawing at six scaled window sizes (fake GUI, not Minecraft)");
  }
}
