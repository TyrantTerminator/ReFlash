package net.minecraft.client.multiplayer;
import java.util.*;
import net.minecraft.world.entity.player.Player;
public class ClientLevel {
  public final List<Player> testPlayers = new ArrayList<>();
  public List<Player> players(){return testPlayers;}
}
