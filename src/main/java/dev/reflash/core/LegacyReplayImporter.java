package dev.reflash.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.zip.ZipFile;

/** Explicit, non-destructive migration for recordings created by the old bundled engine. */
public final class LegacyReplayImporter {
    private LegacyReplayImporter(){}
    public record Result(int imported,int skipped,int invalid){}
    public static Result importRecordings(Path gameDir) throws IOException {
        Path from=gameDir.resolve("momenton").resolve("recordings");
        Path to=VulkanSupport.replayFolder(gameDir);
        if(!Files.isDirectory(from))return new Result(0,0,0);
        Files.createDirectories(to);
        int imported=0,skipped=0,invalid=0;
        try(var files=Files.list(from)){
            for(Path old:files.filter(Files::isRegularFile)
                    .filter(p->p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".momenton")).toList()){
                try(ZipFile zip=new ZipFile(old.toFile())){
                    if(zip.getEntry("metadata.json")==null||zip.getEntry("setup.bin")==null){invalid++;continue;}
                }catch(IOException e){invalid++;continue;}
                String name=old.getFileName().toString();
                name=name.substring(0,name.length()-".momenton".length())+".reflash";
                Path dest=to.resolve(name);
                if(Files.exists(dest)) {skipped++;continue;}
                // Never move/delete the source. Copy to a temporary file then atomically place it.
                Path tmp=to.resolve("."+name+".importing");
                try{
                    Files.copy(old,tmp,StandardCopyOption.REPLACE_EXISTING);
                    try{Files.move(tmp,dest,StandardCopyOption.ATOMIC_MOVE);}
                    catch(AtomicMoveNotSupportedException e){Files.move(tmp,dest);}
                    imported++;
                    Path oldProject=old.resolveSibling(old.getFileName()+".json");
                    Path newProject=dest.resolveSibling(dest.getFileName()+".json");
                    if(Files.isRegularFile(oldProject)&&!Files.exists(newProject)){
                        String edits=Files.readString(oldProject,StandardCharsets.UTF_8)
                                .replace(old.getFileName().toString(),dest.getFileName().toString());
                        Files.writeString(newProject,edits,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
                    }
                }finally{Files.deleteIfExists(tmp);}
            }
        }
        return new Result(imported,skipped,invalid);
    }
}
