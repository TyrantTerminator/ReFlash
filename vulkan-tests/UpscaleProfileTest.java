import dev.reflash.core.*;
import java.nio.file.Files;
public class UpscaleProfileTest {
  public static void main(String[] args)throws Exception{
    var file=Files.createTempDirectory("reflash-upscale-").resolve("config.properties");
    Preferences p=new Preferences(file);
    p.setUpscaleProfile(UpscaleProfile.SHARP);
    p.setOutputPreset(3840,2160,240);
    p.save();
    Preferences reload=new Preferences(file);reload.load();
    if(reload.upscaleProfile()!=UpscaleProfile.SHARP||reload.outputFps()!=240)throw new AssertionError("preset persistence");
    var filter=VideoPostProcessor.filter(3840,2160,240,reload.upscaleProfile());
    if(!filter.contains("flags=lanczos")||!filter.contains("fps=240"))throw new AssertionError("filter settings");
    if(UpscaleProfile.ARCHIVE.crf()>=UpscaleProfile.FAST.crf())throw new AssertionError("quality mapping");
    if(!VideoPostProcessor.filter(1280,720,60,UpscaleProfile.FAST).contains("flags=bilinear"))throw new AssertionError("fast scaling");
    System.out.println("PASS 4 export-quality profiles, 4K240 config and selected scale kernel");
  }
}
