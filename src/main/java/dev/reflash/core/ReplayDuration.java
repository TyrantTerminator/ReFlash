package dev.reflash.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/** Reads recording duration from Momenton's source metadata, NOT from edited clips. */
public final class ReplayDuration {
    private ReplayDuration(){}
    public record Info(int sourceTicks, int editedTicks, boolean complete, String issue){
        public String sourceLabel(){return String.format(java.util.Locale.ROOT,"%.1fs",sourceTicks/20.0);}
        public String editedLabel(){return String.format(java.util.Locale.ROOT,"%.1fs",editedTicks/20.0);}
        public boolean shortened(){return complete && editedTicks < sourceTicks;}
    }
    private static final Pattern DURATION=Pattern.compile("\"durationTicks\"\\s*:\\s*(\\d+)");
    public static Info inspect(Path replay){
        try {
            if(!Files.isRegularFile(replay))return new Info(0,0,false,"Recording file missing");
            try(ZipFile zip=new ZipFile(replay.toFile())){
                var entry=zip.getEntry("metadata.json");
                if(entry==null||entry.getSize()>131072)return new Info(0,0,false,"Missing or oversized metadata");
                String raw;
                try(var stream=zip.getInputStream(entry)) {raw=new String(stream.readNBytes(131073),StandardCharsets.UTF_8);}
                if(raw.length()>131072)return new Info(0,0,false,"Oversized metadata");
                var match=DURATION.matcher(raw);
                if(!match.find())return new Info(0,0,false,"No duration in replay");
                int ticks=Integer.parseInt(match.group(1));
                if(ticks<=0)return new Info(0,0,false,"Recording is empty");
                int edited=ticks;
                try{edited=ReplayProject.open(replay).lengthTicks();}
                catch(Exception ignored){/* source remains inspectable if project is broken */}
                return new Info(ticks,edited,true,"");
            }
        }catch(Exception ex){return new Info(0,0,false,"Archive incomplete: "+ex.getClass().getSimpleName());}
    }
}
