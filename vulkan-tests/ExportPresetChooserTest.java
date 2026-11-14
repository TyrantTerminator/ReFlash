import dev.reflash.core.*;
import dev.reflash.client.*;
import java.lang.reflect.*;
public class ExportPresetChooserTest {
    @SuppressWarnings({"unchecked","rawtypes"})
    public static void main(String[] args)throws Exception {
        var q=ExportSizeEstimate.calculate(1200,1920,1080,60,UpscaleProfile.BALANCED);
        if(!q.available()||Math.abs(q.seconds()-60)>0.01||q.frames()!=3600||q.lowMB()>=q.highMB())throw new AssertionError("estimate basics");
        if(ExportSizeEstimate.calculate(0,1920,1080,60,UpscaleProfile.BALANCED).available())throw new AssertionError("empty replay estimate");
        var better=ExportSizeEstimate.calculate(1200,3840,2160,240,UpscaleProfile.ARCHIVE);
        if(better.lowMB()<=q.lowMB()||better.frames()!=14400)throw new AssertionError("4K240 scaling");
        var prefs=new Preferences(java.nio.file.Files.createTempFile("preset-test", ".properties"));
        prefs.setOutputPreset(2560,1440,144);prefs.setUpscaleProfile(UpscaleProfile.SHARP);prefs.save();
        var loaded=new Preferences(java.nio.file.Files.createTempFile("preset-test-out", ".properties"));
        // Validate dedicated preset controls can render across different Minecraft GUI sizes.
        new ReFlashClient().onInitializeClient();
        var pages=(Class<? extends Enum>)Class.forName("dev.reflash.client.ReFlashScreen$Page");
        Method select=ReFlashScreen.class.getDeclaredMethod("selectPage",pages);select.setAccessible(true);
        Method init=ReFlashScreen.class.getDeclaredMethod("init");init.setAccessible(true);
        for(int[]dim:new int[][]{{320,240},{480,270},{640,360},{960,540},{1280,720}}){
            var screen=new ReFlashScreen(null);screen.width=dim[0];screen.height=dim[1];init.invoke(screen);
            select.invoke(screen,Enum.valueOf(pages,"EXPORT"));
            if(screen.widgets.size()<5)throw new AssertionError("Preset widgets absent "+dim[0]);
            for(var b:screen.widgets)if(b.getX()<0||b.getY()<0||b.getX()+b.getWidth()>dim[0]||b.getY()+b.getHeight()>dim[1])throw new AssertionError("Offscreen preset at "+dim[0]);
        }
        System.out.println("PASS approximate size estimates, output FPS choices and responsive preset selectors");
    }
}
