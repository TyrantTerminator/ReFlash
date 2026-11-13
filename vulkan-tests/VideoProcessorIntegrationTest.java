import dev.reflash.core.VideoPostProcessor;
import dev.reflash.core.Preferences;
import java.nio.file.*;
public class VideoProcessorIntegrationTest {
    public static void main(String[] args) throws Exception {
        Path game=Files.createTempDirectory("reflash-video-test");
        Path dir=Files.createDirectories(game.resolve("momenton/exports"));
        Path fixture=dir.resolve("source.mp4");
        Process sample=new ProcessBuilder("ffmpeg","-hide_banner","-loglevel","error","-y","-f","lavfi",
                "-i","color=c=purple:s=160x90:r=20","-t","0.50","-c:v","mpeg4","-q:v","6",fixture.toString())
                .inheritIO().start();
        if(sample.waitFor()!=0||Files.size(fixture)<100)throw new AssertionError("Cannot prepare fixture");
        Preferences pref=new Preferences(game.resolve("config.properties"));
        pref.setOutputPreset(1280,720,60);
        if(!VideoPostProcessor.startLatest(game,pref))throw new AssertionError(VideoPostProcessor.status());
        for(int i=0;i<500 && VideoPostProcessor.running();i++)Thread.sleep(50);
        if(VideoPostProcessor.running())throw new AssertionError("Processor did not complete");
        if(VideoPostProcessor.lastOutput()==null||Files.size(VideoPostProcessor.lastOutput())==0)
            throw new AssertionError("Processor did not save output: "+VideoPostProcessor.status());
        System.out.println("PASS: " + VideoPostProcessor.lastOutput()+" " + VideoPostProcessor.status());
    }
}
