package com.relapzz.momenton.playback;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
public class ReplaySession {
  public static boolean running;
  public static Path file;
  public static boolean isPlaying() {return running;}
  public static void open(Minecraft client,Path p) {file=p;running=true;}
  public static Path file(){return file;}
  public static int durationTicks(){return 2400;}
}
