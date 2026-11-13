import dev.reflash.client.ReFlashClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
public class VulkanClientContractTest {
  public static void main(String[] args){
    new ReFlashClient().onInitializeClient();
    if (KeyBindingHelper.registrations!=4)throw new AssertionError("Expected 4 ReFlash key bindings");
    if(ClientTickEvents.END_CLIENT_TICK.listener==null)throw new AssertionError("Missing tick callback");
    if(ClientPlayConnectionEvents.JOIN.listener==null||ClientPlayConnectionEvents.DISCONNECT.listener==null)throw new AssertionError("Missing connection callbacks");
    System.out.println("PASS: actual Vulkan ReFlashClient compiles and registers bindings/events using test interfaces");
  }
}
