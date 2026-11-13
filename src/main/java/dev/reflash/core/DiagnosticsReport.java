package dev.reflash.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/** Privacy-minimizing support report: no user name, server IP, chat, account or absolute paths. */
public final class DiagnosticsReport {
    private DiagnosticsReport(){}
    public static Path create(Path gameDir,Preferences prefs,boolean backendPresent,
                              String duplicateKey,PerformanceSampler.Result fps,boolean ffmpeg){
        Path folder=gameDir.resolve("reflash");Path target=folder.resolve("diagnostics.txt");
        try{
            Files.createDirectories(folder);
            String s="ReFlash diagnostic report (safe to share after review)\n"+
               "Generated: "+Instant.now()+"\n"+
               "ReFlash version: 0.10.0-vulkan-alpha\n"+
               "Minecraft target: 1.21.11 Fabric\n"+
               "OS family: "+safeOs()+"\n"+
               "Java major: "+Runtime.version().feature()+"\n"+
               "Momenton engine detected: "+backendPresent+"\n"+
               "Independent Momenton record key: "+allowedKey(duplicateKey)+"\n"+
               "FFmpeg accessible: "+ffmpeg+"\n"+
               "Recording indicator: "+prefs.hudPosition()+"\n"+
               "Recording mode: manual\n"+
               "Output target: "+prefs.outputLabel()+" (upscaled when above Vulkan window size)\n"+
               "Benchmark sample counts: off="+fps.idleSamples()+", on="+fps.recordingSamples()+"\n"+
               "Benchmark average FPS: off="+Math.round(fps.idleFps())+", on="+Math.round(fps.recordingFps())+"\n"+
               "NOTE: report intentionally excludes usernames, servers, chats, devices, file paths and replay content.\n";
            Path tmp=target.resolveSibling("diagnostics.txt.tmp");
            Files.writeString(tmp,s,StandardCharsets.UTF_8);
            try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}
            return target;
        }catch(IOException e){return null;}
    }
    private static String allowedKey(String name){
        if(name==null||!name.matches("[a-zA-Z0-9_ +.-]{1,32}"))return "other";
        return name;
    }
    private static String safeOs(){
        String os=System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT);
        if(os.contains("win"))return "Windows";
        if(os.contains("mac"))return "macOS";
        if(os.contains("linux"))return "Linux";
        return "Other";
    }
}
