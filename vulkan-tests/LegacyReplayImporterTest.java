import dev.reflash.core.LegacyReplayImporter;
import dev.reflash.core.ReplayIndex;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
public class LegacyReplayImporterTest {
    public static void main(String[] args) throws Exception {
        Path home=Files.createTempDirectory("reflash-import-");
        Path old=home.resolve("momenton/recordings");Files.createDirectories(old);
        Path sample=old.resolve("survival.momenton");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(sample))){
            out.putNextEntry(new ZipEntry("metadata.json"));out.write("{\"durationTicks\":400}".getBytes(StandardCharsets.UTF_8));out.closeEntry();
            out.putNextEntry(new ZipEntry("setup.bin"));out.write(new byte[]{1,2,3});out.closeEntry();
        }
        Path sidecar=old.resolve("survival.momenton.json");
        Files.writeString(sidecar,"{\"replay\":\"survival.momenton\"}");
        var result=LegacyReplayImporter.importRecordings(home);
        if(result.imported()!=1||result.skipped()!=0||result.invalid()!=0)throw new AssertionError(result);
        Path dest=home.resolve("reflash/recordings/survival.reflash");
        if(!Files.isRegularFile(dest)||!Files.isRegularFile(sample))throw new AssertionError("Non-destructive copy failed");
        if(!Files.readString(home.resolve("reflash/recordings/survival.reflash.json")).contains("survival.reflash"))
            throw new AssertionError("Clip project did not follow imported replay");
        if(ReplayIndex.recent(home.resolve("reflash/recordings"),10).size()!=1)throw new AssertionError("Import invisible to library");
        if(LegacyReplayImporter.importRecordings(home).skipped()!=1)throw new AssertionError("Importer overwrote existing replay");
        System.out.println("PASS: legacy archive import, sidecar retention and repeat-safe non-destructive behavior");
    }
}
