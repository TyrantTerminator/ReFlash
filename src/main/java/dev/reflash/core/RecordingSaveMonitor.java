package dev.reflash.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

/** 
 * Status monitor only. Does NOT touch or reconstruct Momenton's file writer.
 * Uses periodic (<1Hz) filesystem reads, never per rendered frame.
 */
public final class RecordingSaveMonitor {
    public enum State{IDLE,SAVING,SAVED,UNCONFIRMED}
    private State state=State.IDLE;
    private final Path replayFolder;
    private final Set<String> before=new HashSet<>();
    private String lastFile="";
    private int checks,unchanged;
    private int expectedTicks, savedTicks;
    private boolean armed;
    private long candidateSize=-1;
    public RecordingSaveMonitor(Path replayFolder){this.replayFolder=replayFolder;}
    public State state(){return state;}
    public String lastFile(){return lastFile;}
    public int savedTicks(){return savedTicks;}
    public boolean shortened(){return expectedTicks>=20 && savedTicks>0 && savedTicks+20<expectedTicks;}
    public void expectTicks(int recordedGameTicks){expectedTicks=Math.max(0,recordedGameTicks);}
    /** Capture the pre-recording list once, before Momenton writes the replay file. */
    public void arm(){
        if(armed)return;
        before.clear();
        try{for(var r:ReplayIndex.recent(replayFolder,10000))before.add(r.fileName());}
        catch(IOException ignored){}
        armed=true;
    }
    public void begin(){
        if(state==State.SAVED||state==State.UNCONFIRMED)clear();
        arm();
        checks=0;unchanged=0;candidateSize=-1;lastFile="";savedTicks=0;state=State.SAVING;
    }
    /** Must be called at most once per second (every 20 game ticks). */
    public State check(){
        if(state!=State.SAVING)return state;
        if(++checks>90){state=State.UNCONFIRMED;return state;}
        try {
            for(var r:ReplayIndex.recent(replayFolder,100)){
                if(before.contains(r.fileName()))continue;
                if(r.byteSize()<128)continue;
                if(r.byteSize()!=candidateSize||!r.fileName().equals(lastFile)){
                    candidateSize=r.byteSize();lastFile=r.fileName();unchanged=0;continue;
                }
                if(++unchanged>=2&&validReplay(r.path())){
                    savedTicks=ReplayDuration.inspect(r.path()).sourceTicks();
                    if(savedTicks<1)continue;
                    state=State.SAVED;return state;
                }
            }
        }catch(IOException ignored){}
        return state;
    }
    private static boolean validReplay(Path path){
        if(!Files.isRegularFile(path))return false;
        try(ZipFile zip=new ZipFile(path.toFile())){
            var entry=zip.getEntry("metadata.json");
            return entry!=null&&entry.getSize()!=0;
        }catch(IOException e){return false;}
    }
    public void clear(){state=State.IDLE;armed=false;}
}
