import dev.reflash.client.ReFlashReplayBridge;
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
    check(ReFlashReplayBridge.backendPresent(),"Momenton API resolved");
    check(!ReFlashReplayBridge.isRecording(),"initial recording idle");
    check(ReFlashReplayBridge.toggleRecording(client),"start recording");
    check(Recorder.running && ReFlashReplayBridge.isRecording(),"recorder state observed");
    Recorder.elapsed=260;
    check(ReFlashReplayBridge.markHighlight(client),"local sidecar bookmark");
    check(Files.exists(Path.of("reflash/bookmarks.tsv")),"bookmark persisted");
    check(ReFlashReplayBridge.toggleRecording(client),"stop recording");
    check(!ReFlashReplayBridge.markHighlight(client),"no bookmark outside recording");
    check(ReFlashReplayBridge.openReplayBrowser(client,null) && client.screen instanceof com.relapzz.momenton.ui.ReplayBrowserScreen,"browser opened");
    Path dir=Files.createTempDirectory("reflash-vulkan-test");
    Path file=dir.resolve("example.reflash");Files.writeString(file,"test");
    check(ReFlashReplayBridge.openReplay(client,file),"native replay opened");
    check(ReFlashReplayBridge.isInReplay() && ReplaySession.file.equals(file),"playback state");
    check(ReFlashReplayBridge.openNativeEditor(client) && EditSession.toggles==1,"native editor opened");
    check(ReFlashReplayBridge.openReplayForEditing(client,file),"queued editor from selected replay");
    ReFlashReplayBridge.onClientTick(client);
    check(EditSession.toggles==2,"queued editor opens when replay is active");
    check(ReFlashReplayBridge.openNativeExport(client,null) && client.screen instanceof com.relapzz.momenton.ui.ExportScreen,"native export opens");
    check(VulkanSupport.isReplayFile(file),"native replay extension");
    check(!VulkanSupport.isReplayFile(dir.resolve("foo.zip")),"Flashback zip excluded");
    check(VulkanSupport.replayFolder(dir).equals(dir.resolve("reflash/recordings")),"correct library directory");
    check(!VulkanSupport.canRenderResolution(2560,1440,1366,768,true),"oversized Vulkan export cannot be promised");
    check(VulkanSupport.canRenderResolution(1366,768,1366,768,true),"native size passes");
    check(VulkanSupport.canRenderResolution(2560,1440,1366,768,false),"no Vulkan limit for other renderers");
    Files.createDirectories(VulkanSupport.replayFolder(dir));
    Files.writeString(VulkanSupport.replayFolder(dir).resolve("1.reflash"),"data");
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
    Files.deleteIfExists(Path.of("reflash/bookmarks.tsv"));Files.deleteIfExists(Path.of("reflash/recordings"));
    Files.deleteIfExists(Path.of("reflash/diagnostics/recording-events.tsv"));
    Files.deleteIfExists(Path.of("reflash/diagnostics"));Files.deleteIfExists(Path.of("reflash"));
    System.out.println("PASS: ReFlash integrated replay, bookmark, replay, browser, editor, export, Vulkan output guard, replay index");
  }
}
