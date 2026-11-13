package com.relapzz.momenton.export;import net.minecraft.client.Minecraft;import net.minecraft.client.gui.screens.Screen;
public final class ExportSession {public static boolean running;public static ExportSettings last;public static boolean isExporting(){return running;}
public static void start(Minecraft c,ExportSettings settings,Screen parent){last=settings;running=true;}
public static void cancel(){running=false;}}
