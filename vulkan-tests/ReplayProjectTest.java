import dev.reflash.core.ReplayProject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReplayProjectTest {
    public static void main(String[] args) throws Exception {
        var tmp = Files.createTempDirectory("reflash-project-test-");
        var replay = tmp.resolve("testing.momenton");
        try(var z = new ZipOutputStream(Files.newOutputStream(replay))){
            z.putNextEntry(new ZipEntry("metadata.json"));
            z.write("{\"durationTicks\": 2400, \"micStartTick\": 42.5}".getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        var project = ReplayProject.open(replay);
        if (project.sourceTicks() != 2400 || !project.hasMicrophoneTrack()) throw new AssertionError();
        project.trimStart(0, 200);
        project.trimEnd(0, 2000);
        if(!project.split(0))throw new AssertionError();
        if(project.clips().size()!=2)throw new AssertionError();
        project.save();
        var loaded = ReplayProject.open(replay);
        if(loaded.clips().size()!=2 || loaded.lengthTicks()!=1800)throw new AssertionError("reload");
        loaded.delete(1);
        loaded.save();
        if(!Files.exists(ReplayProject.projectPath(replay).resolveSibling("testing.momenton.json.reflash-backup")))throw new AssertionError("backup");
        System.out.println("PASS ReFlash project editor creates compatible clip JSON and backups");
    }
}
