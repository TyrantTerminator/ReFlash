import dev.reflash.core.SafeMediaRename;
import java.nio.file.*;
public final class SafeMediaRenameTest {
  public static void main(String[] args) throws Exception {
    Path root=Files.createTempDirectory("reflash-rename-test");
    Path source=root.resolve("demo.reflash");
    Path sidecar=root.resolve("demo.reflash.json");
    Files.writeString(source,"test replay");Files.writeString(sidecar,"edit project");
    Path renamed=SafeMediaRename.rename(root,source,"My Demo 01",true);
    if(!renamed.equals(root.resolve("My Demo 01.reflash")))throw new AssertionError("replay rename");
    if(!Files.exists(root.resolve("My Demo 01.reflash.json")))throw new AssertionError("sidecar rename");
    if(!SafeMediaRename.suggestedName(renamed).equals("My Demo 01"))throw new AssertionError("suggestion");
    try{SafeMediaRename.rename(root,renamed,"../escape",true);throw new AssertionError("unsafe rename accepted");}
    catch(java.io.IOException expected){}
    Path movie=root.resolve("clip.mp4");Files.writeString(movie,"video");
    if(!SafeMediaRename.rename(root,movie,"Final Edit",false).equals(root.resolve("Final Edit.mp4")))
       throw new AssertionError("video rename");
    System.out.println("PASS SafeMediaRenameTest");
  }
}
