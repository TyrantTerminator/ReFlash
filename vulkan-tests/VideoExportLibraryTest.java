import dev.reflash.core.VideoExportLibrary;
import dev.reflash.core.VideoPostProcessor;
import dev.reflash.core.Preferences;
import java.nio.file.Files;
public class VideoExportLibraryTest {
  public static void main(String[]args)throws Exception{
    var temp=Files.createTempDirectory("reflash-exportfiles");var folder=VideoExportLibrary.folder(temp);Files.createDirectories(folder);
    var a=folder.resolve("one.mp4");var b=folder.resolve("two.mkv");var intermediate=folder.resolve("reflash-video-temp.mp4");
    Files.writeString(a,"a");Thread.sleep(10);Files.writeString(b,"b");Files.writeString(intermediate,"tmp");
    var x=VideoExportLibrary.list(temp,10);
    if(x.size()!=2||!x.getFirst().fileName().equals("two.mkv"))throw new AssertionError("Filtering or ordering wrong: "+x);
    if(!VideoExportLibrary.allowed(temp,a)||VideoExportLibrary.allowed(temp,intermediate))throw new AssertionError("Allowed paths failed");
    var other=Files.createTempFile("other",".mp4");
    if(VideoExportLibrary.allowed(temp,other))throw new AssertionError("External file accepted");
    var prefs=new Preferences(temp.resolve("prefs"));
    if(VideoPostProcessor.startSelected(temp,other,prefs))throw new AssertionError("External file processed");
    if(!VideoPostProcessor.status().contains("Select"))throw new AssertionError("Missing selection hint");
    System.out.println("PASS safe, explicit exported-video selection and non-destructive target paths");
  }
}
