package dev.reflash.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.nio.file.Path;
public class FlashbackBridge {
 public static boolean isRecording(){return false;}
 public static boolean isInReplay(){return true;}
 public static boolean openReplayEditor(Minecraft c,Screen s){return true;}
 public static boolean openReplay(Minecraft c,Path p){return true;}
 public static void toggleRecording(Minecraft c){}
 public static void markHighlight(Minecraft c){}
}
