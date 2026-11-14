import dev.reflash.core.ReplayProject;
import dev.reflash.core.ReplayDuration;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReplayDurationTest {
  public static void main(String[] args) throws Exception {
    var dir=Files.createTempDirectory("reflash-duration-");
    var replay=dir.resolve("sample.reflash");
    try(var zip=new ZipOutputStream(Files.newOutputStream(replay))){
      zip.putNextEntry(new ZipEntry("metadata.json"));
      zip.write("{\"durationTicks\":212}".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }
    var original=ReplayDuration.inspect(replay);
    if(!original.complete()||original.sourceTicks()!=212||original.editedTicks()!=212||original.shortened())throw new AssertionError("wrong original range");
    ReplayProject project=ReplayProject.open(replay);project.trimEnd(0,140);project.save();
    var cut=ReplayDuration.inspect(replay);
    if(cut.sourceTicks()!=212||cut.editedTicks()!=140||!cut.shortened())throw new AssertionError("source vs edited range not separated");
    project.reset();project.save();
    if(ReplayDuration.inspect(replay).shortened())throw new AssertionError("restore full duration failed");
    var broken=dir.resolve("broken.reflash");Files.writeString(broken,"not a zip");
    if(ReplayDuration.inspect(broken).complete())throw new AssertionError("broken archive accepted");
    System.out.println("PASS 10.6s original vs 7.0s edit range, restore, invalid archive");
  }
}
