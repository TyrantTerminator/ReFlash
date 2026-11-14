import dev.reflash.client.NativeReplayExporter;
import dev.reflash.client.ReFlashExportProgressScreen;
import dev.reflash.client.ReFlashScreen;
import net.minecraft.client.Minecraft;
import java.lang.reflect.Field;
public class ExportSkinRoutingTest {
  public static class FakeFinisher {
    boolean done, cancelled;
    double progress=.42;
    public boolean done(){return done;}
    public boolean cancelled(){return cancelled;}
    public double progress(){return progress;}
    public Throwable error(){return null;}
    public void cancel(){cancelled=true;done=true;}
  }
  public static void main(String[]args)throws Exception{
    new dev.reflash.client.ReFlashClient().onInitializeClient();
    Minecraft m=Minecraft.getInstance();
    ReFlashScreen parent=new ReFlashScreen(null);
    Field active=NativeReplayExporter.class.getDeclaredField("reflashExportActive");active.setAccessible(true);
    Field ref=NativeReplayExporter.class.getDeclaredField("reflashParent");ref.setAccessible(true);
    ref.set(null,parent);active.setBoolean(null,true);
    m.setScreen(new com.relapzz.momenton.ui.ExportFailedScreen());
    NativeReplayExporter.routeExportScreen(m);
    if(!(m.screen instanceof ReFlashExportProgressScreen)||active.getBoolean(null))throw new AssertionError("Failure page not skinned");
    ref.set(null,parent);active.setBoolean(null,true);
    FakeFinisher f=new FakeFinisher();
    m.setScreen(new com.relapzz.momenton.ui.ExportFinishingScreen(f));
    NativeReplayExporter.routeExportScreen(m);
    if(!(m.screen instanceof ReFlashExportProgressScreen)||active.getBoolean(null))throw new AssertionError("Finishing page not skinned");
    m.screen.tick();
    if(f.cancelled)throw new AssertionError("Should not cancel a running export");
    m.screen.onClose();
    if(!f.cancelled)throw new AssertionError("Cancel didn't propagate");
    // The user may press Export inside Momenton's editor after opening it from ReFlash.
    // That entry point must return to our custom Export tab rather than the vanilla screen.
    var init=ReFlashScreen.class.getDeclaredMethod("init");init.setAccessible(true);
    init.invoke(parent);
    Field editOrigin=dev.reflash.client.ReFlashReplayBridge.class.getDeclaredField("studioEditorOrigin");editOrigin.setAccessible(true);
    editOrigin.set(null,parent);
    m.setScreen(new com.relapzz.momenton.ui.ExportScreen(null,java.nio.file.Path.of("demo.reflash"),200));
    dev.reflash.client.ReFlashReplayBridge.onClientTick(m);
    if(m.screen!=parent)throw new AssertionError("Native edit-export didn't return to ReFlash");
    Field page=ReFlashScreen.class.getDeclaredField("page");page.setAccessible(true);
    if(!page.get(parent).toString().equals("EXPORT"))throw new AssertionError("Not in ReFlash Export tab");
    System.out.println("PASS ReFlash-owned native editor export, failure/finishing screens reroute to Studio");
  }
}
