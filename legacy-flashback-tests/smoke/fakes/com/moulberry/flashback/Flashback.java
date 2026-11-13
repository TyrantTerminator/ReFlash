package com.moulberry.flashback;
import com.moulberry.flashback.playback.ReplayServer;
public class Flashback {
  public static ReplayServer server = new ReplayServer();
  public static final FakeConfig config = new FakeConfig();
  public static ReplayServer getReplayServer() { return server; }
  public static FakeConfig getConfig() { return config; }
  public static final class FakeConfig {
    public final FakeExport internalExport = new FakeExport();
    public final FakeExport forceDefaultExportSettings = new FakeExport();
  }
  public static final class FakeExport {
    public int[] resolution = null;
    public float[] framerate = null;
  }
}
