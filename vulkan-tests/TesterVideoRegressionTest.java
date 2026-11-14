package dev.reflash.client;

import dev.reflash.core.StudioLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

import javax.imageio.ImageIO;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Deterministic regressions visible in the 2026-11-14 friend test footage. */
public final class TesterVideoRegressionTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        String assets="src/main/resources/assets/reflash/textures/gui/";
        for (int size : new int[] {35,37}) {
            var logo=ImageIO.read(Path.of(assets+"reflash_mark_"+size+".png").toFile());
            if(logo==null||logo.getWidth()!=size||logo.getHeight()!=size)
                throw new AssertionError("Native logo size is wrong; will repeat under Minecraft UV blit: "+size);
        }
        var small=ImageIO.read(Path.of(assets+"reflash_button.png").toFile());
        if(small.getWidth()!=16 || small.getHeight()!=16)throw new AssertionError("Compact menu logo dimensions");
        new ReFlashClient().onInitializeClient();
        var mc=Minecraft.getInstance();
        var oldLevel=mc.level;var oldPlayer=mc.player;
        Class pageClass=Class.forName("dev.reflash.client.ReFlashScreen$Page");
        Method select=ReFlashScreen.class.getDeclaredMethod("selectPage",pageClass);select.setAccessible(true);
        Field controls=ReFlashScreen.class.getDeclaredField("controls");controls.setAccessible(true);
        try {
            // On the title screen the Start Recording control is visible but disabled.
            mc.level=null;mc.player=null;
            var welcome=new ReFlashScreen(null);welcome.width=1120;welcome.height=630;welcome.init();
            Button record=find((List<?>)controls.get(welcome),"Start Recording");
            if(record==null || record.active)throw new AssertionError("Main menu must not pretend recording is ready");
            mc.level=oldLevel;mc.player=oldPlayer;
            var library=new ReFlashScreen(null);library.width=1120;library.height=630;library.init();
            select.invoke(library,Enum.valueOf(pageClass,"LIBRARY"));
            var layout=StudioLayout.forViewport(1120,630);
            Button refresh=find((List<?>)controls.get(library),"Refresh");
            Button importOld=find((List<?>)controls.get(library),"Import Old");
            if(refresh==null || importOld==null)throw new AssertionError("Library toolbar missing");
            int toolbarStart=layout.contentY()+39;
            int contentStart=layout.contentY()+74;
            if(refresh.getY()<toolbarStart || importOld.getY()<toolbarStart ||
                    refresh.getY()+refresh.getHeight()>=contentStart ||
                    importOld.getY()+importOld.getHeight()>=contentStart)
                throw new AssertionError("Library actions overlap heading or recordings panel");
            // Newly saved recording appears automatically even if Studio is already open.
            Path folder=Path.of("reflash/recordings");Files.createDirectories(folder);
            Path replay=folder.resolve("reflash-regression-tester-sample.reflash");
            try {
                var home=new ReFlashScreen(null);home.width=1120;home.height=630;home.init();
                try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(replay))){
                    zip.putNextEntry(new ZipEntry("metadata.json"));
                    zip.write("{\"durationTicks\":400}".getBytes());zip.closeEntry();
                }
                for(int i=0;i<40;i++)home.tick();
                Field list=ReFlashScreen.class.getDeclaredField("replayList");list.setAccessible(true);
                boolean found=((List<?>)list.get(home)).stream().anyMatch(x->x.toString().contains(replay.getFileName().toString()));
                if(!found)throw new AssertionError("Studio didn't refresh after a saved replay appears");
            } finally {Files.deleteIfExists(replay);}
            System.out.println("PASS friend-video regressions: correct logos, non-overlapping Library, safe Start button, new recording refresh");
        } finally {mc.level=oldLevel;mc.player=oldPlayer;}
    }
    private static Button find(List<?> controls,String wanted) throws Exception {
        for(Object c:controls){
            Method cap=c.getClass().getDeclaredMethod("caption");cap.setAccessible(true);
            String text=String.valueOf(((Supplier<?>)cap.invoke(c)).get());
            if(!text.equals(wanted))continue;
            Method widget=c.getClass().getDeclaredMethod("widget");widget.setAccessible(true);
            return (Button)widget.invoke(c);
        }
        return null;
    }
}
