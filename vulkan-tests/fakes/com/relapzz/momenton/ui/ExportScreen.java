package com.relapzz.momenton.ui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
public class ExportScreen extends Screen {
  public ExportScreen(Screen parent,Path file,int durationTicks) {super(Component.literal("Export"));}
}
