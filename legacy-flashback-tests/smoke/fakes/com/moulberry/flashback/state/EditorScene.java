package com.moulberry.flashback.state;
import com.moulberry.flashback.keyframe.Keyframe;
import java.util.*;
import java.util.function.Consumer;
public class EditorScene {
 public String name="Opening";
 public EditorScene(){}
 public EditorScene(String name){this.name=name;}
 public final List<KeyframeTrack> keyframeTracks=new ArrayList<>();
 public int exportStartTicks=-1,exportEndTicks=-1;
 public void setKeyframe(int track,int tick,Keyframe keyframe){keyframeTracks.get(track).keyframesByTick.put(tick,keyframe);}
 public void undo(Consumer<String> consumer){ consumer.accept("undo"); }
 public void redo(Consumer<String> consumer){ consumer.accept("redo"); }
 public void setExportTicks(int a,int b,int total){exportStartTicks=a;exportEndTicks=b;}
}
