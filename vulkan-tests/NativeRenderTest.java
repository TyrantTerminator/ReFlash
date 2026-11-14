import dev.reflash.client.NativeReplayExporter;
import dev.reflash.core.ReplayProject;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
public class NativeRenderTest {
 public static void main(String[]args)throws Exception{
    var root=Files.createTempDirectory("native-render-test-");var replay=root.resolve("test.reflash");
    try(var z=new ZipOutputStream(Files.newOutputStream(replay))){z.putNextEntry(new ZipEntry("metadata.json"));z.write("{\"durationTicks\":200}".getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    int[] fb=NativeReplayExporter.framebuffer(net.minecraft.client.Minecraft.getInstance());
    if(fb==null||fb[0]!=1600||fb[1]!=900)throw new AssertionError("window dimensions");
    var project=ReplayProject.open(replay);project.splitAt(0,70);
    // FFmpeg fake: a real executable is not needed to validate the bridge constructor; inspect all argument types separately.
    Class<?> cls=Class.forName("com.relapzz.momenton.export.ExportSettings");
    if(cls.getConstructors().length!=1)throw new AssertionError();
    if(!cls.getRecordComponents()[4].getName().equals("fps"))throw new AssertionError("settings mismatch");
    if(NativeReplayExporter.render(net.minecraft.client.Minecraft.getInstance(),null,root,project,241))throw new AssertionError("Invalid fps accepted");
    var tools=root.resolve("ffmpeg/bin");Files.createDirectories(tools);
    var executable=tools.resolve("ffmpeg.exe");Files.writeString(executable,"#!/bin/sh\nexit 0\n");executable.toFile().setExecutable(true);
    if(!NativeReplayExporter.render(net.minecraft.client.Minecraft.getInstance(),null,root,project,120))throw new AssertionError(NativeReplayExporter.status());
    var settings=com.relapzz.momenton.export.ExportSession.last;
    if(settings.width()!=1600||settings.height()!=900||settings.fps()!=120||settings.clips().size()!=2||settings.clips().get(0).endTick()!=70)
        throw new AssertionError("Export settings handoff mismatch: "+settings);
    NativeReplayExporter.cancel();
    if(com.relapzz.momenton.export.ExportSession.running)throw new AssertionError("Cancellation failed");
    System.out.println("PASS native render settings bridge with clip cuts, 120fps and Vulkan window dimensions (simulated)");
 }
}
