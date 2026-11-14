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
    ReFlashClient.toggleBenchmark();
    if(!ReFlashClient.performanceSampler().active())throw new AssertionError("F12 sampling did not start");
    ReFlashClient.toggleBenchmark();
    if(ReFlashClient.performanceSampler().active())throw new AssertionError("F12 sampling did not stop");
    if(!ReFlashClient.managerKeyName().equals("F7"))throw new AssertionError("Manager default key not registered");
    if(!ReFlashClient.engineRecordKey().startsWith("Disabled"))
      throw new AssertionError("Integrated engine still exposes an old recorder key");
    if(!ReFlashClient.manageEngineRecordingKey())
      throw new AssertionError("ReFlash recorder key compatibility handler failed");
    for(var screen:new net.minecraft.client.gui.screens.Screen[]{new TitleScreen(),new PauseScreen()}){
      ScreenEvents.AFTER_INIT.fire(game,screen,800,450);
      if(screen.widgets.size()!=1)throw new AssertionError("Expected one tiny button on "+screen.getClass());
      var button=screen.widgets.get(0);
      if(button.getWidth()!=102||button.getHeight()!=27)throw new AssertionError("Dedicated ReFlash button must be 102x27");
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
    game.setScreen(null);
    com.relapzz.momenton.record.Recorder.running=true;
    com.relapzz.momenton.record.Recorder.elapsed=1300;
    ClientTickEvents.END_CLIENT_TICK.listener.accept(game);
    if(!ReFlashClient.cachedRecording() || ReFlashClient.cachedRecordingTicks()!=1300)
      throw new AssertionError("HUD recorder status is not refreshed on client tick");
    var hud=net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.Holder.last;
    if(hud==null)throw new AssertionError("HUD was not registered");
    var pixels=new net.minecraft.client.gui.GuiGraphics(800,450);
    hud.render(pixels,null);
    if(pixels.paintCount<3)throw new AssertionError("Recording HUD is not visible");
    ReFlashClient.preferences().setHudPosition(dev.reflash.core.Preferences.HudPosition.OFF);
    var off=new net.minecraft.client.gui.GuiGraphics(800,450);
    hud.render(off,null);
    if(off.paintCount!=0)throw new AssertionError("Recording HUD OFF setting not honored");
    System.out.println("PASS: actual Vulkan ReFlashClient compiles and registers bindings/events using test interfaces");
  }
}
