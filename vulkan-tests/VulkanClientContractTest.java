import dev.reflash.client.ReFlashClient;
import dev.reflash.client.ReFlashScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
public class VulkanClientContractTest {
  public static void main(String[] args){
    new ReFlashClient().onInitializeClient();
    if (KeyBindingHelper.registrations!=4)throw new AssertionError("Expected 4 ReFlash key bindings");
    if(ClientTickEvents.END_CLIENT_TICK.listener==null)throw new AssertionError("Missing tick callback");
    if(ClientPlayConnectionEvents.JOIN.listener==null||ClientPlayConnectionEvents.DISCONNECT.listener==null)throw new AssertionError("Missing connection callbacks");
    Minecraft game=Minecraft.getInstance();
    if(!ReFlashClient.managerKeyName().equals("F7"))throw new AssertionError("Manager default key not registered");
    for(var screen:new net.minecraft.client.gui.screens.Screen[]{new TitleScreen(),new PauseScreen()}){
      ScreenEvents.AFTER_INIT.fire(game,screen,800,450);
      if(screen.widgets.size()!=1)throw new AssertionError("Expected one tiny button on "+screen.getClass());
      var button=screen.widgets.get(0);
      if(button.getWidth()!=22||button.getHeight()!=22)throw new AssertionError("Icon is not 22x22");
      ScreenEvents.afterRender(screen).fire(screen,new net.minecraft.client.gui.GuiGraphics(800,450));
      button.click();
      if(!(game.screen instanceof ReFlashScreen))throw new AssertionError("Icon did not open Manager");
    }
    // The manager must open Minecraft's real keybind screen, not just display instructions.
    ReFlashScreen manager=new ReFlashScreen(null);
    try {
      var meth=ReFlashScreen.class.getDeclaredMethod("openKeybinds");
      meth.setAccessible(true);
      meth.invoke(manager);
    } catch(Exception ex){throw new AssertionError("Settings -> keybinds callback failed",ex);}
    if(!(game.screen instanceof net.minecraft.client.gui.screens.options.controls.KeyBindsScreen))
      throw new AssertionError("Change Keybinds does not open the native controls screen");
    System.out.println("PASS: actual Vulkan ReFlashClient compiles and registers bindings/events using test interfaces");
  }
}
