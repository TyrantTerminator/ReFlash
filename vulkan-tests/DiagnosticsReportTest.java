import dev.reflash.core.*;
import java.nio.file.Files;
public class DiagnosticsReportTest{
 public static void main(String[]args)throws Exception{
    var temp=Files.createTempDirectory("reflash-diag-");
    var p=new Preferences(temp.resolve("prefs"));
    var result=new PerformanceSampler().result();
    var out=DiagnosticsReport.create(temp,p,true,"F9",result,true);
    if(out==null||!Files.exists(out))throw new AssertionError("No report");
    var text=Files.readString(out);
    if(!text.contains("Internal replay engine detected: true")||text.contains(temp.toString()))throw new AssertionError("Status missing or path leaked");
    if(!text.contains("Recording key ownership: F9"))throw new AssertionError("No key info");
    System.out.println("PASS privacy-filtered local diagnostic report");
 }
}
