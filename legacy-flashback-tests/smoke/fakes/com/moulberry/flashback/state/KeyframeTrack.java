package com.moulberry.flashback.state;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import java.util.TreeMap;
public class KeyframeTrack {
 public final KeyframeType keyframeType;
 public TreeMap<Integer,Keyframe> keyframesByTick=new TreeMap<>();
 public KeyframeTrack(KeyframeType t){keyframeType=t;}
}
