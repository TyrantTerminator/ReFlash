import dev.reflash.core.RecordingSaveMonitor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
public class RecordingSaveMonitorTest{
 public static void main(String[]args)throws Exception{
    var root=Files.createTempDirectory("reflash-save");var monitor=new RecordingSaveMonitor(root);
    monitor.begin();if(monitor.state()!=RecordingSaveMonitor.State.SAVING)throw new AssertionError();
    var replay=root.resolve("test.momenton");
    try(var z=new ZipOutputStream(Files.newOutputStream(replay))){z.putNextEntry(new ZipEntry("metadata.json"));z.write("{\"durationTicks\":100}".getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    for(int i=0;i<5;i++)monitor.check();
    if(monitor.state()!=RecordingSaveMonitor.State.SAVED)throw new AssertionError("Valid ZIP not acknowledged: "+monitor.state());
    if(!monitor.lastFile().equals("test.momenton"))throw new AssertionError();
    monitor.begin();
    for(int i=0;i<100;i++)monitor.check();
    if(monitor.state()!=RecordingSaveMonitor.State.UNCONFIRMED)throw new AssertionError("Save timeout not surfaced");
    System.out.println("PASS complete-file validation, no false saved status, timed-out save warning");
 }
}
