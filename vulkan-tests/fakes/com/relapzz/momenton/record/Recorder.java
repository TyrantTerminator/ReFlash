package com.relapzz.momenton.record;
import net.minecraft.client.Minecraft;
public class Recorder {
    public static boolean running;
    public static int elapsed;
    public static boolean isRecording() { return running; }
    public static void toggle(Minecraft client) { running=!running; }
    public static int elapsedTicks() { return elapsed; }
}
