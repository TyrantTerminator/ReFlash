package dev.reflash.client;

import dev.reflash.core.StudioLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.*;

/** Regression for player-visible UI: real editor route, compact FPS and native export interception. */
public final class EditorUXRegressionTest {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void main(String[]args)throws Exception{
        new ReFlashClient().onInitializeClient();
        Path dir=Path.of("reflash/recordings");Files.createDirectories(dir);
        Path replay=dir.resolve("reflash-fps-editor-test.reflash");
        try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(replay))){
            z.putNextEntry(new ZipEntry("metadata.json"));
            z.write("{\"durationTicks\":600}".getBytes());z.closeEntry();
        }
        try{
            Class<?> page=Class.forName("dev.reflash.client.ReFlashScreen$Page");
            Method select=ReFlashScreen.class.getDeclaredMethod("selectPage",page);select.setAccessible(true);
            Method edit=ReFlashScreen.class.getDeclaredMethod("editSelectedReplay");edit.setAccessible(true);
            Field current=ReFlashScreen.class.getDeclaredField("page");current.setAccessible(true);
            Field selected=ReFlashScreen.class.getDeclaredField("selectedReplay");selected.setAccessible(true);
            Field items=ReFlashScreen.class.getDeclaredField("replayList");items.setAccessible(true);
            Field controls=ReFlashScreen.class.getDeclaredField("controls");controls.setAccessible(true);
            var studio=new ReFlashScreen(null);studio.width=1120;studio.height=630;studio.init();
            select.invoke(studio,Enum.valueOf((Class<Enum>)page,"LIBRARY"));
            List<?> replays=(List<?>)items.get(studio);
            int index=-1;
            for(int i=0;i<replays.size();i++) if(replays.get(i).toString().contains(replay.getFileName().toString()))index=i;
            check(index>=0,"Test recording missing from ReFlash Library");
            selected.set(studio,index);
            Minecraft client=Minecraft.getInstance();
            var before=client.screen;
            edit.invoke(studio);
            check("EDITOR".equals(current.get(studio).toString()),"Edit Selected must open ReFlash Editor");
            check(client.screen==before,"Edit Selected must not open engine's legacy editor");
            check(!((List<?>)controls.get(studio)).isEmpty(),"Editor widgets missing");
            // Editing shortcuts must not disappear at common Minecraft GUI scales.
            for(int[] dimension:new int[][]{{480,270},{854,480},{1120,630}}){
                var editor=new ReFlashScreen(null);editor.width=dimension[0];editor.height=dimension[1];editor.init();
                select.invoke(editor,Enum.valueOf((Class<Enum>)page,"EDITOR"));
                Set<String> labels=new HashSet<>();
                for(Object c:(List<?>)controls.get(editor)){
                    Method cap=c.getClass().getDeclaredMethod("caption");cap.setAccessible(true);
                    labels.add(String.valueOf(((Supplier<?>)cap.invoke(c)).get()));
                }
                check(labels.contains("Split") && (labels.contains("Save Cuts")||labels.contains("Save cuts")),
                    "Editor Split/Save inaccessible at "+dimension[0]+"x"+dimension[1]);
            }
            // FPS must be immediately clickable on smallest GUI, without opening SIZE tab.
            for(int[] dimension:new int[][]{{320,240},{480,270},{854,480},{1120,630}}){
                var ex=ReFlashScreen.exporting(null,replay);ex.width=dimension[0];ex.height=dimension[1];ex.init();
                Set<String> visible=new HashSet<>();
                for(Object control:(List<?>)controls.get(ex)){
                    Method cap=control.getClass().getDeclaredMethod("caption");cap.setAccessible(true);
                    visible.add(String.valueOf(((Supplier<?>)cap.invoke(control)).get()));
                }
                for(int rate:new int[]{30,60,120,144,240})
                    check(visible.contains(rate+" FPS"),"FPS "+rate+" hidden at "+dimension[0]+"x"+dimension[1]);
                check("EXPORT".equals(current.get(ex).toString()),"Export not the opening page");
            }
            // Native engine Export screen cannot take over the UI, even without editor origin.
            var fakeScreen=new com.relapzz.momenton.ui.ExportScreen(null,replay,600);
            client.setScreen(fakeScreen);
            ReFlashReplayBridge.onClientTick(client);
            check(client.screen instanceof ReFlashScreen,"Legacy export form wasn't intercepted");
            check("EXPORT".equals(current.get(client.screen).toString()),"Redirect didn't open Export Studio");
            client.screen.onClose();
            check(!(client.screen instanceof com.relapzz.momenton.ui.ExportScreen),
                "Closing custom exporter must not reopen legacy export dialog");
            System.out.println("PASS real ReFlash Editor navigation, all FPS choices immediately visible, legacy export intercepted");
        }finally{Files.deleteIfExists(replay);}
    }
}
