import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.tree.analysis.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
public final class Verify74 {
  static final String[] CLASSES={
    "dev/reflash/core/Preferences", "dev/reflash/core/ExportSizeEstimate", "dev/reflash/core/ReFlashMotionFilter",
    "dev/reflash/core/VideoPostProcessor", "dev/reflash/client/NativeReplayExporter",
    "dev/reflash/client/ReFlashCustomOutputScreen", "dev/reflash/client/ReFlashVideoExportScreen",
    "dev/reflash/client/ReFlashTextEntryScreen",
    "dev/reflash/client/ReFlashScreen", "dev/reflash/client/ReFlashExportPanel60",
    "dev/reflash/client/ReFlashAboutUI60", "dev/reflash/client/ReFlashFps74",
    "dev/reflash/client/ReFlashBeta74", "dev/reflash/client/ReFlashResponsive74"};
  public static void main(String[]args)throws Exception {
    int methods=0;
    try(ZipFile jar=new ZipFile(args[0])){
      for(String cls:CLASSES){
        var entry=jar.getEntry(cls+".class");if(entry==null)throw new Error("Missing "+cls);
        ClassNode c=new ClassNode();new ClassReader(jar.getInputStream(entry).readAllBytes()).accept(c,0);
        for(MethodNode m:c.methods){
          if(m.instructions==null||m.instructions.size()==0)continue;
          try {new Analyzer<>(new BasicVerifier()).analyze(c.name,m);}
          catch(AnalyzerException e){throw new AssertionError(cls+"."+m.name+m.desc+": "+e,e);}
          methods++;
        }
      }
    }
    System.out.println("PASS ASM bytecode analysis: "+CLASSES.length+" classes, "+methods+" methods");
  }
}
