package dev.reflash.client;
import net.minecraft.client.gui.GuiGraphics;
import java.nio.file.Path;
public class RenderExportPolish {
    public static class FakeFinisher {
        public boolean done(){return false;}
        public boolean cancelled(){return false;}
        public double progress(){return .72;}
        public Exception error(){return null;}
        public void cancel(){}
    }
    public static void main(String[]args)throws Exception {
        ReFlashExportProgressScreen[] screens={
            ReFlashExportProgressScreen.finishing(new ReFlashScreen(null),new FakeFinisher()),
            ReFlashExportProgressScreen.failed(new ReFlashScreen(null),"FFmpeg encoder couldn't start. Verify FFmpeg in the export settings.")
        };
        String[] names={"FINISHING","ERROR"};
        for(int i=0;i<screens.length;i++){
            var s=screens[i];s.width=1120;s.height=630;s.init();s.tick();
            GuiGraphics g=new GuiGraphics(1120,630);s.render(g,0,0,0);
            g.save(Path.of("/mnt/data/ReFlash-v0.10.2-preview/EXPORT-"+names[i]+".png"));
        }
    }
}
