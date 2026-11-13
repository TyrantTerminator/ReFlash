package dev.reflash.core;

import java.util.List;

/** Maps timeline pixel positions to source ticks in a non-destructive multi-clip project. */
public final class EditorTimeline {
    private EditorTimeline(){}
    public record Position(int clipIndex,int sourceTick,int timelineTick){}
    public static Position at(List<ReplayProject.Clip> clips,int totalWidth,int pixel){
        if(clips==null||clips.isEmpty()||totalWidth<=0)throw new IllegalArgumentException("Empty timeline");
        int total=clips.stream().mapToInt(ReplayProject.Clip::length).sum();
        int offset=(int)Math.min(total-1,Math.max(0,(long)Math.clamp(pixel,0,totalWidth)*total/totalWidth));
        int elapsed=0;
        for(int i=0;i<clips.size();i++){
            var c=clips.get(i);
            if(offset<elapsed+c.length())return new Position(i,c.startTick()+offset-elapsed,offset);
            elapsed+=c.length();
        }
        var last=clips.getLast();return new Position(clips.size()-1,last.endTick()-1,total-1);
    }
    public static int pixelAt(List<ReplayProject.Clip> clips,int totalWidth,int selectedClip,int sourceTick){
        if(clips==null||clips.isEmpty()||totalWidth<=0)throw new IllegalArgumentException("Empty timeline");
        long cumulative=0,total=0;
        for(var c:clips)total+=c.length();
        for(int i=0;i<selectedClip;i++)cumulative+=clips.get(i).length();
        var c=clips.get(selectedClip);
        return (int)((cumulative+Math.clamp(sourceTick,c.startTick(),c.endTick())-c.startTick())*totalWidth/total);
    }
}
