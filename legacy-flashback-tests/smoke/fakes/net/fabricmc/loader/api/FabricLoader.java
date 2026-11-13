package net.fabricmc.loader.api;
import java.nio.file.Path;
public class FabricLoader {
  private static final FabricLoader INSTANCE = new FabricLoader();
  public static FabricLoader getInstance(){return INSTANCE;}
  public Path getGameDir(){return Path.of(".");} public Path getConfigDir(){return Path.of(".");}
}
