package dev.reflash.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/** Non-destructive .momenton clip editing. Writes only a Momenton-compatible sidecar project. */
public final class ReplayProject {
    public record Clip(int startTick, int endTick) {
        public Clip { if (startTick < 0 || endTick <= startTick) throw new IllegalArgumentException("Invalid clip"); }
        public int length() { return endTick - startTick; }
    }
    private static final Pattern DURATION = Pattern.compile("\\\"durationTicks\\\"\\s*:\\s*(\\d+)");
    private static final Pattern MIC = Pattern.compile("\\\"micStartTick\\\"\\s*:\\s*([0-9.eE+-]+)");
    private static final Pattern CLIP = Pattern.compile("\\{\\s*\\\"startTick\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"endTick\\\"\\s*:\\s*(\\d+)\\s*\\}");
    private static final int HISTORY_LIMIT = 60;
    private final Path replay;
    private final int sourceTicks;
    private final boolean microphoneTrack;
    private final List<Clip> clips;
    private final Deque<List<Clip>> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private boolean dirty;

    private ReplayProject(Path replay, int sourceTicks, boolean microphoneTrack, List<Clip> clips) {
        this.replay=replay; this.sourceTicks=sourceTicks; this.microphoneTrack=microphoneTrack;
        this.clips=new ArrayList<>(clips);
    }
    public static Path projectPath(Path replay){return replay.resolveSibling(replay.getFileName()+".json");}
    public Path replayPath(){return replay;}
    public int sourceTicks(){return sourceTicks;}
    public boolean hasMicrophoneTrack(){return microphoneTrack;}
    public List<Clip> clips(){return List.copyOf(clips);}
    public int lengthTicks(){return clips.stream().mapToInt(Clip::length).sum();}
    public boolean dirty(){return dirty;}
    public boolean canUndo(){return !undo.isEmpty();}
    public boolean canRedo(){return !redo.isEmpty();}

    public static ReplayProject open(Path replay) throws IOException {
        if (!Files.isRegularFile(replay)||!replay.getFileName().toString().endsWith(".momenton"))
            throw new IOException("Not a Momenton replay");
        String metadata;
        try (ZipFile zip=new ZipFile(replay.toFile())){
            var entry=zip.getEntry("metadata.json");
            if(entry==null)throw new IOException("Replay metadata missing");
            try(var in=zip.getInputStream(entry)){metadata=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        }
        Matcher duration=DURATION.matcher(metadata);
        if(!duration.find())throw new IOException("Replay duration missing");
        int ticks;
        try{ticks=Integer.parseInt(duration.group(1));}catch(NumberFormatException e){throw new IOException("Invalid replay duration",e);}
        if(ticks<=0)throw new IOException("Replay contains no playable ticks");
        List<Clip> cuts=new ArrayList<>();
        Path project=projectPath(replay);
        if(Files.isRegularFile(project)){
            String existing=Files.readString(project);
            Matcher match=CLIP.matcher(existing);
            while(match.find()){
                try{
                    int start=Integer.parseInt(match.group(1)),end=Integer.parseInt(match.group(2));
                    if(end<=ticks&&start>=0&&start<end)cuts.add(new Clip(start,end));
                }catch(NumberFormatException ignore){}
            }
        }
        if(cuts.isEmpty())cuts.add(new Clip(0,ticks));
        return new ReplayProject(replay,ticks,MIC.matcher(metadata).find(),cuts);
    }

    private void checkpoint(){
        undo.addLast(List.copyOf(clips));
        if(undo.size()>HISTORY_LIMIT)undo.removeFirst();
        redo.clear();dirty=true;
    }
    public boolean undo(){
        if(undo.isEmpty())return false;
        redo.addLast(List.copyOf(clips));clips.clear();clips.addAll(undo.removeLast());dirty=true;return true;
    }
    public boolean redo(){
        if(redo.isEmpty())return false;
        undo.addLast(List.copyOf(clips));clips.clear();clips.addAll(redo.removeLast());dirty=true;return true;
    }
    public void trimStart(int index,int tick){
        Clip c=clips.get(index);int target=Math.clamp(tick,0,c.endTick()-1);
        if(target==c.startTick())return;
        checkpoint();clips.set(index,new Clip(target,c.endTick()));
    }
    public void trimEnd(int index,int tick){
        Clip c=clips.get(index);int target=Math.clamp(tick,c.startTick()+1,sourceTicks);
        if(target==c.endTick())return;
        checkpoint();clips.set(index,new Clip(c.startTick(),target));
    }
    /** A split at an explicit source tick, which must be strictly inside the selected clip. */
    public boolean splitAt(int index,int sourceTick){
        Clip c=clips.get(index);
        if(sourceTick<=c.startTick()||sourceTick>=c.endTick())return false;
        checkpoint();clips.set(index,new Clip(c.startTick(),sourceTick));
        clips.add(index+1,new Clip(sourceTick,c.endTick()));return true;
    }
    /** Legacy midpoint operation retained for compatibility; the editor uses splitAt. */
    public boolean split(int index){Clip c=clips.get(index);return splitAt(index,c.startTick()+c.length()/2);}
    public boolean delete(int index){
        if(clips.size()<=1)return false;
        clips.get(index);checkpoint();clips.remove(index);return true;
    }
    public boolean move(int from,int to){
        if(from<0||from>=clips.size()||to<0||to>=clips.size())return false;
        if(from==to)return false;
        checkpoint();clips.add(to,clips.remove(from));return true;
    }
    public boolean reset(){
        if(clips.size()==1&&clips.getFirst().equals(new Clip(0,sourceTicks)))return false;
        checkpoint();clips.clear();clips.add(new Clip(0,sourceTicks));return true;
    }
    /** Saves atomically and retains a backup of the previously saved edits. Never modifies .momenton. */
    public void save() throws IOException {
        Path file=projectPath(replay);
        if(Files.isRegularFile(file)){
            Path backup=file.resolveSibling(file.getFileName()+".reflash-backup");
            if(!Files.exists(backup))Files.copy(file,backup);
        }
        StringBuilder json=new StringBuilder();
        json.append("{\n  \"replay\": \"").append(escape(replay.getFileName().toString()))
            .append("\",\n  \"timeline\": {\n    \"sourceTicks\": ").append(sourceTicks).append(",\n    \"clips\": [\n");
        for(int i=0;i<clips.size();i++){
            Clip c=clips.get(i);
            json.append("      {\"startTick\": ").append(c.startTick()).append(", \"endTick\": ").append(c.endTick()).append("}");
            if(i<clips.size()-1)json.append(',');json.append('\n');
        }
        json.append("    ]\n  }\n}\n");
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        Files.writeString(tmp,json.toString(),StandardCharsets.UTF_8);
        try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(java.nio.file.AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        dirty=false;
    }
    private static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
}
