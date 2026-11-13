package dev.reflash.client;
import dev.reflash.core.Preferences;
import dev.reflash.core.PerformanceSampler;
import java.nio.file.Path;
public class ReFlashClient {
 private static final Preferences p=new Preferences(Path.of("/tmp/reflash-fake.properties"));
 private static final PerformanceSampler meter=new PerformanceSampler();
 public static void notifyPlayer(String s){}
 public static Preferences preferences(){return p;}
 public static void savePreferences(){}
 public static PerformanceSampler performanceSampler(){return meter;}
 public static void toggleBenchmark(){if(meter.active())meter.stop();else meter.start();}
}
