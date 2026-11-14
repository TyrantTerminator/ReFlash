package dev.reflash.client;

import dev.reflash.core.FfmpegCheck;
import dev.reflash.core.ReplayProject;
import dev.reflash.core.VideoExportLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Experimental native offline rendering through Momenton ExportSession.
 * The custom ReFlash preset UI creates Momenton's settings directly instead of displaying
 * its vanilla-looking ExportScreen. This uses Vulkan's *actual framebuffer dimensions*;
 * requested 4K output is done separately by ReFlash's clearly labeled post-processor.
 */
public final class NativeReplayExporter {
    private NativeReplayExporter(){}
    private static final String BASE="com.relapzz.momenton.";
    private static volatile String status="Ready";
    // Route ONLY exports initiated inside ReFlash; Momenton's own screens remain untouched.
    private static volatile boolean reflashExportActive;
    private static volatile Screen reflashParent;
    public static void routeExportScreen(Minecraft client) {
        if(!reflashExportActive || client.screen == null) return;
        String name=client.screen.getClass().getName();
        if(name.equals(BASE+"ui.ExportFailedScreen")) {
            Screen parent=reflashParent;
            reflashExportActive=false;
            client.setScreen(ReFlashExportProgressScreen.failed(parent,
                "The replay encoder could not finish. Check reflash/export.log for details."));
        } else if(name.equals(BASE+"ui.ExportFinishingScreen")) {
            try {
                java.lang.reflect.Field field=client.screen.getClass().getDeclaredField("finisher");
                field.setAccessible(true);
                Object finisher=field.get(client.screen);
                Screen parent=reflashParent;
                reflashExportActive=false;
                client.setScreen(ReFlashExportProgressScreen.finishing(parent,finisher));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                // Never interrupt a valid export if Momenton's internal API changes.
                status="ReFlash progress skin unavailable; native finisher is still active";
                reflashExportActive=false;
            }
        }
    }

    public static String status(){return status;}
    public static boolean isRendering(){
        try{return Boolean.TRUE.equals(Class.forName(BASE+"export.ExportSession").getMethod("isExporting").invoke(null));}
        catch(ReflectiveOperationException|LinkageError e){return false;}
    }
    public static void cancel(){
        try{Class.forName(BASE+"export.ExportSession").getMethod("cancel").invoke(null);status="Cancelling render";}
        catch(ReflectiveOperationException|LinkageError e){status="Cannot cancel this render";}
    }
    public static int[] framebuffer(Minecraft client){
        // Mojang official mappings: Window.getWidth()/getHeight() are framebuffer pixels.
        // Compile-time calls are remapped by Loom; reflection string method names are NOT.
        int w=client.getWindow().getWidth()&~1;
        int h=client.getWindow().getHeight()&~1;
        if(w<2||h<2){status="Invalid framebuffer size";return null;}
        return new int[]{w,h};
    }
    public static boolean render(Minecraft client,Screen parent,Path gameDir,ReplayProject project,int fps){
        if(project==null||fps<24||fps>240||isRendering()){
            status="Select a replay and FPS 24-240; avoid parallel exports";return false;
        }
        if(!FfmpegCheck.available(gameDir)){
            status="FFmpeg missing. Install it and restart your launcher";return false;
        }
        try{
            int[] size=framebuffer(client);
            if(size==null)return false;
            Path exportDir=VideoExportLibrary.folder(gameDir);
            Files.createDirectories(exportDir);
            String stem=project.replayPath().getFileName().toString().replaceFirst("(?i)\\.reflash$","");
            String prefix=stem+"-native-"+size[0]+"x"+size[1]+"-"+fps+"fps";
            Path output=exportDir.resolve(prefix+".mp4");
            for(int i=1;Files.exists(output)&&i<1000;i++)output=exportDir.resolve(prefix+"-"+i+".mp4");
            if(Files.exists(output)){status="Too many exports with same name";return false;}
            Class<?> clipClass=Class.forName(BASE+"edit.Clip");
            var clipCtor=clipClass.getConstructor(int.class,int.class);
            List<Object> nativeClips=new ArrayList<>();
            for(var clip:project.clips())nativeClips.add(clipCtor.newInstance(clip.startTick(),clip.endTick()));
            Class<?> presetClass=Class.forName(BASE+"export.EncodingPreset");
            @SuppressWarnings({"rawtypes","unchecked"})
            Object preset=Enum.valueOf((Class<? extends Enum>)presetClass.asSubclass(Enum.class),"MP4_DEFAULT");
            String args=(String)presetClass.getMethod("arguments").invoke(preset);
            Class<?> settingsClass=Class.forName(BASE+"export.ExportSettings");
            Object nativeSettings=settingsClass.getConstructor(Path.class,List.class,int.class,int.class,int.class,
                    Path.class,String.class,String.class,int.class,boolean.class,float.class,float.class)
                    .newInstance(project.replayPath(),nativeClips,size[0],size[1],fps,output,
                            FfmpegCheck.candidate(gameDir),args,0,false,(ReFlashClient.preferences()==null || ReFlashClient.preferences().includeGameAudio())?1.0f:0.0f,project.hasMicrophoneTrack()?1.0f:0.0f);
            Class<?> session=Class.forName(BASE+"export.ExportSession");
            // Momenton manages replay loading, world audio and rendering on its own.
            reflashParent=parent;
            reflashExportActive=true;
            session.getMethod("start",Minecraft.class,settingsClass,Screen.class).invoke(null,client,nativeSettings,parent);
            if(!isRendering()){
                status="Render did not start. See reflash/export.log or latest.log";return false;
            }
            status="Rendering "+output.getFileName()+" (native window size)";
            return true;
        }catch(InvocationTargetException ex){
            Throwable reason=ex.getCause()==null?ex:ex.getCause();
            status="Native render failed: "+reason.getClass().getSimpleName()+": "+reason.getMessage();return false;
        }catch(ReflectiveOperationException|java.io.IOException|LinkageError ex){
            status="Native render unavailable: "+ex.getMessage();return false;
        }
    }
}
