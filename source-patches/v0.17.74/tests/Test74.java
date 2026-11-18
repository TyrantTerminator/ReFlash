import dev.reflash.core.Preferences;
import dev.reflash.core.ExportSizeEstimate;
import dev.reflash.core.ReFlashMotionFilter;
import dev.reflash.core.VideoPostProcessor;
import dev.reflash.core.UpscaleProfile;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Test74 {
  static int checks=0;
  static void ok(boolean p,String m){checks++;if(!p)throw new AssertionError(m);}
  static boolean allows(Preferences p,int fps){try {p.setOutputPreset(1280,720,fps); return true;}catch(IllegalArgumentException e){return false;}}
  public static void main(String[] args)throws Exception {
    Path dir=Files.createTempDirectory("reflash-fps74-");
    Preferences p=new Preferences(dir.resolve("settings.properties"));
    ok(!allows(p,0),"0 fps invalid");
    ok(allows(p,1),"1 fps must be accepted");
    ok(allows(p,2),"2 fps accepted");
    ok(allows(p,30),"30 fps accepted");
    ok(allows(p,239),"239 fps accepted");
    ok(allows(p,240),"240 fps accepted");
    ok(!allows(p,241),"241 fps rejected");
    p.setOutputPreset(1280,720,1); p.save();
    Preferences p2=new Preferences(dir.resolve("settings.properties"));p2.load();
    ok(p2.outputFps()==1,"1 fps should survive reload");
    var profile=UpscaleProfile.values()[0];
    ok(ExportSizeEstimate.calculate(200,1280,720,1,profile)!=null,"size estimator 1 fps");
    ok(ReFlashMotionFilter.filter(1280,720,1).contains("fps=1") || !ReFlashMotionFilter.filter(1280,720,1).isBlank(),"motion filter accepts 1");
    ok(VideoPostProcessor.filter(1280,720,1,profile)!=null,"video processing filter accepts 1");
    String helper="dev.reflash.client.ReFlashFps74";
    Class<?> c=Class.forName(helper);
    var parse=c.getDeclaredMethod("parse",String.class);parse.setAccessible(true);
    ok((int)parse.invoke(null,"1")==1,"parse minimum");
    ok((int)parse.invoke(null,"240")==240,"parse maximum");
    ok((int)parse.invoke(null," 17 ")==17,"parse arbitrary fps");
    for(String bad: new String[]{"0","241","abc","","1.5","-2"}) {
      try {parse.invoke(null,bad);throw new AssertionError("accepted invalid "+bad);}catch(java.lang.reflect.InvocationTargetException e){ok(e.getCause() instanceof IllegalArgumentException,"reject invalid "+bad);}
    }
    Class<?> b=Class.forName("dev.reflash.client.ReFlashBeta74");
    b.getDeclaredMethod("draw",Object.class,Object.class);
    b.getDeclaredMethod("mouse",Object.class,Object.class);
    Class<?> rs=Class.forName("dev.reflash.client.ReFlashResponsive74");
    var overlaps=rs.getDeclaredMethod("overlaps",int.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class);
    overlaps.setAccessible(true);
    ok((boolean)overlaps.invoke(null,0,0,70,20,65,2,70,20),"overlap detected");
    ok(!(boolean)overlaps.invoke(null,0,0,70,20,75,2,70,20),"gap not overlap");
    System.out.println("PASS " + checks + " ReFlash74 checks");
  }
}
