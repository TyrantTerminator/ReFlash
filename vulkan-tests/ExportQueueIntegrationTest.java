import dev.reflash.core.VideoPostProcessor;
import dev.reflash.core.Preferences;
import java.nio.file.*;
public class ExportQueueIntegrationTest{
  public static void main(String[]args)throws Exception{
    Path fixture=Files.createTempFile("reflash-test-input-", ".mp4");
    Process sample = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi",
        "-i", "color=c=blue:s=160x90:r=20", "-t", "0.50", "-c:v", "mpeg4", "-q:v", "6", fixture.toString())
        .inheritIO().start();
    if(sample.waitFor()!=0 || Files.size(fixture)<100)throw new AssertionError("Could not generate test input");
    Path game=Files.createTempDirectory("reflash-queue-video");Path exports=Files.createDirectories(game.resolve("momenton/exports"));
    Path a=exports.resolve("alpha.mp4"),b=exports.resolve("beta.mp4");Files.copy(fixture,a);Files.copy(fixture,b);
    Preferences prefs=new Preferences(game.resolve("prefs"));prefs.setOutputPreset(640,360,30);
    if(!VideoPostProcessor.startBatch(game,java.util.List.of(a,b),prefs))throw new AssertionError(VideoPostProcessor.status());
    if(VideoPostProcessor.startSelected(game,a,prefs))throw new AssertionError("Parallel export accepted");
    for(int i=0;i<600&&VideoPostProcessor.running();i++)Thread.sleep(50);
    if(VideoPostProcessor.running())throw new AssertionError("Timed out");
    if(VideoPostProcessor.jobsDone()!=2||VideoPostProcessor.jobsTotal()!=2)throw new AssertionError("Queue incomplete: "+VideoPostProcessor.status());
    if(Files.list(exports).filter(p->p.getFileName().toString().contains("-ReFlash-")).count()!=2)throw new AssertionError("Expected two outputs");
    System.out.println("PASS two real FFmpeg exports queued and rendered serially");
  }
}
