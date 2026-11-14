package dev.reflash.client;

import net.minecraft.client.Minecraft;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Runs a GPL-derived replay engine compiled to intermediary mappings inside the
 * single ReFlash mod. The engine's own entrypoint is deliberately NOT registered,
 * so it cannot add a second title button, recording HUD or F9 recording binding.
 * This does not load or depend on a separately installed mod.
 */
public final class EmbeddedReplayRuntime {
    private static final Logger LOG=Logger.getLogger("ReFlash/ReplayEngine");
    private static final String ROOT="com.relapzz.momenton.";
    private static final String[] TASKS={"record.ScreenTracker", "record.Recorder",
            "playback.ReplaySession", "ui.editor.EditSession", "audio.MicPlayback"};
    private static Method[] methods;
    private static boolean warned;
    private EmbeddedReplayRuntime(){}
    public static void bootstrap(){
        try {
            Class.forName(ROOT+"ExportTemporaries").getMethod("deleteOrphans").invoke(null);
            methods=new Method[TASKS.length];
            for(int i=0;i<TASKS.length;i++){
                methods[i]=Class.forName(ROOT+TASKS[i]).getMethod("onClientTick", Minecraft.class);
            }
        } catch(ReflectiveOperationException|LinkageError error){
            methods=null;
            LOG.warning("ReFlash replay engine unavailable: "+error);
        }
    }
    public static boolean ready(){return methods!=null;}
    public static void tick(Minecraft client){
        Method[] active=methods;
        if(active==null)return;
        try{
            for(Method m:active)m.invoke(null,client);
        }catch(ReflectiveOperationException|LinkageError error){
            methods=null;
            if(!warned){warned=true;LOG.warning("ReFlash replay engine tick failed: "+error);}
        }
    }
}
