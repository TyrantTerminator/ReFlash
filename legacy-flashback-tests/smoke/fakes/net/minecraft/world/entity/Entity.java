package net.minecraft.world.entity;
import java.util.UUID;
import net.minecraft.network.chat.Component;
public class Entity {
  private final UUID id;
  private final String name;
  public Entity(){ this(UUID.randomUUID(), "Viewer"); }
  public Entity(UUID id, String name){this.id=id;this.name=name;}
  public UUID getUUID(){return id;}
  public Component getName(){return Component.literal(name);}
}
