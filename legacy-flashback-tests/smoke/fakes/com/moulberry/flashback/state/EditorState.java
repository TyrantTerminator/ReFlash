package com.moulberry.flashback.state;
import java.util.*;
public class EditorState {
 private final List<EditorScene> scenes=new ArrayList<>(List.of(new EditorScene(),new EditorScene()));
 private int selected=0;
 public int dirty=0;
 public long acquireRead(){return 1L;}
 public long acquireWrite(){return 1L;}
 public void release(long stamp){}
 public EditorScene getCurrentScene(long stamp){return scenes.get(selected);}
 public List<EditorScene> getScenes(long stamp){return scenes;}
 public int getSceneIndex(){return selected;}
 public void setSceneIndex(int i,long stamp){selected=i;}
 public void markDirty(){dirty++;}
}
