package dev.reflash.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Explicit selection of actual exported videos; excludes intermediate FFmpeg files. */
public final class VideoExportLibrary {
    private VideoExportLibrary(){}
    public record Media(Path path,String fileName,long size,Instant modifiedAt){}
    public static Path folder(Path gameDir){return gameDir.resolve("momenton").resolve("exports");}
    public static List<Media> list(Path gameDir,int limit)throws IOException{
        if(limit<=0||!Files.isDirectory(folder(gameDir)))return List.of();
        try(var stream=Files.list(folder(gameDir))){
            return stream.filter(Files::isRegularFile)
                .filter(VideoExportLibrary::valid)
                .map(p->{try{return new Media(p,p.getFileName().toString(),Files.size(p),Files.getLastModifiedTime(p).toInstant());}
                    catch(IOException e){return null;}})
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Media::modifiedAt).reversed()).limit(limit).toList();
        }
    }
    public static boolean valid(Path p){
        if(p==null)return false;
        String n=p.getFileName().toString().toLowerCase(Locale.ROOT);
        return (n.endsWith(".mp4")||n.endsWith(".mkv")||n.endsWith(".mov"))
            && !n.startsWith("momenton-video-") && !n.startsWith("momenton-audio-");
    }
    /** Don't allow accidentally processing another directory or unrelated user file. */
    public static boolean allowed(Path gameDir,Path p)throws IOException{
        return p!=null && Files.isRegularFile(p) && valid(p)
            && p.toRealPath().getParent().equals(folder(gameDir).toRealPath());
    }
}
