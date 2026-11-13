package com.moulberry.flashback.playback;
public class ReplayServer {
  public volatile boolean replayPaused = true;
  public int tick=60,total=1200;
  public int getReplayTick(){return tick;}
  public int getTotalReplayTicks(){return total;}
  public void goToReplayTick(int tick){this.tick=tick;}
  public float desiredSpeed=20f;
  public void setDesiredTickRate(float speed,boolean manual){desiredSpeed=speed;}
}
