package net.fabricmc.fabric.api.client.screen.v1;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
public class Screens {
 public static List<Button> getButtons(Screen screen){return screen.widgets;}
}
