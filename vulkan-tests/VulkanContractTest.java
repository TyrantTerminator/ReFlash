import dev.reflash.client.MomentonBridge;
import dev.reflash.core.VulkanSupport;
import dev.reflash.core.ReplayIndex;
import dev.reflash.core.VideoPostProcessor;
import dev.reflash.core.Preferences;
import com.relapzz.momenton.record.Recorder;
import com.relapzz.momenton.playback.ReplaySession;
import com.relapzz.momenton.ui.editor.EditSession;
import net.minecraft.client.Minecraft;
import java.nio.file.Files;
import java.nio.file.Path;
public class VulkanContractTest {
  static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
  public static void main(String[] args)throws Exception {
    Minecraft client=Minecraft.getInstance();
    check(MomentonBridge.backendPresent(),"Momenton API resolved");
    check(!MomentonBridge.isRecording(),"initial recording idle");
    check(MomentonBridge.toggleRecording(client),"start recording");
    check(Recorder.running && MomentonBridge.isRecording(),"recorder state observed");
    Recorder.elapsed=260;
    check(MomentonBridge.markHighlight(client),"local sidecar bookmark");
    check(Files.exists(Path.of("reflash/bookmarks.tsv")),"bookmark persisted");
    check(MomentonBridge.toggleRecording(client),"stop recording");
    check(!MomentonBridge.markHighlight(client),"no bookmark outside recording");
    check(MomentonBridge.openReplayBrowser(client,null) && client.screen instanceof com.relapzz.momenton.ui.ReplayBrowserScreen,"browser opened");
    Path dir=Files.createTempDirectory("reflash-vulkan-test");
    Path file=dir.resolve("example.momenton");Files.writeString(file,"test");
    check(MomentonBridge.openReplay(client,file),"native replay opened");
    check(MomentonBridge.isInReplay() && ReplaySession.file.equals(file),"playback state");
    check(MomentonBridge.openNativeEditor(client) && EditSession.toggles==1,"native editor opened");
    check(MomentonBridge.openReplayForEditing(client,file),"queued editor from selected replay");
    MomentonBridge.onClientTick(client);
    check(EditSession.toggles==2,"queued editor opens when replay is active");
    check(MomentonBridge.openNativeExport(client,null) && client.screen instanceof com.relapzz.momenton.ui.ExportScreen,"native export opens");
    check(VulkanSupport.isReplayFile(file),"native replay extension");
    check(!VulkanSupport.isReplayFile(dir.resolve("foo.zip")),"Flashback zip excluded");
    check(VulkanSupport.replayFolder(dir).equals(dir.resolve("momenton/recordings")),"correct library directory");
    check(!VulkanSupport.canRenderResolution(2560,1440,1366,768,true),"oversized Vulkan export cannot be promised");
    check(VulkanSupport.canRenderResolution(1366,768,1366,768,true),"native size passes");
    check(VulkanSupport.canRenderResolution(2560,1440,1366,768,false),"no Vulkan limit for other renderers");
    Files.createDirectories(VulkanSupport.replayFolder(dir));
    Files.writeString(VulkanSupport.replayFolder(dir).resolve("1.momenton"),"data");
    Files.writeString(VulkanSupport.replayFolder(dir).resolve("ignore.zip"),"data");
    check(ReplayIndex.recent(VulkanSupport.replayFolder(dir),10).size()==1,"library ignores wrong format");
    check(VideoPostProcessor.filter(3840,2160,240).contains("fps=240"),"4K240 offline format filter");
    try { VideoPostProcessor.filter(4096,2160,240); throw new AssertionError("maximum exceeded"); }
    catch(IllegalArgumentException expected) { }
    var config=new Preferences(dir.resolve("preferences.properties"));
    config.setHudPosition(Preferences.HudPosition.TOP_LEFT);
    config.setOutputPreset(3840,2160,240);config.save();
    var restored=new Preferences(dir.resolve("preferences.properties"));restored.load();
    check(restored.hudPosition()==Preferences.HudPosition.TOP_LEFT,"HUD position persistence");
    check(restored.outputWidth()==3840&&restored.outputFps()==240,"4K240 settings persistence");
    Files.deleteIfExists(Path.of("reflash/bookmarks.tsv"));Files.deleteIfExists(Path.of("reflash"));
    System.out.println("PASS: Momenton native recording, bookmark, replay, browser, editor, export, Vulkan output guard, replay index");
  }
}
