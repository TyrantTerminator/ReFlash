import dev.reflash.core.Preferences;
import dev.reflash.core.ExportPreset;
import dev.reflash.core.ReplayIndex;
import dev.reflash.core.PerformanceSampler;
import dev.reflash.core.StudioLayout;
import dev.reflash.core.PauseMenuLayout;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class SmokeTest {
    public static void main(String[] args) throws Exception {
        Path d = Files.createTempDirectory("reflash-smoke-");
        try {
            Preferences preferences = new Preferences(d.resolve("cfg/reflash.properties"));
            if (preferences.autoRecord() || preferences.exportPreset() != ExportPreset.QHD120)
                throw new AssertionError("default recording/export settings incorrect");
            preferences.setAutoRecord(true);
            preferences.setNotifications(false);
            preferences.setExportPreset(ExportPreset.UHD60);
            preferences.save();
            Preferences reopened = new Preferences(d.resolve("cfg/reflash.properties"));
            reopened.load();
            if (!reopened.autoRecord() || reopened.notifications() || reopened.exportPreset() != ExportPreset.UHD60)
                throw new AssertionError("settings round-trip failed");
            if (ExportPreset.fromId("garbage") != ExportPreset.HD60) throw new AssertionError("invalid preset fallback");
            if (ExportPreset.QHD120.frameCount(60) != 7200) throw new AssertionError("120fps frame math failed");
            if (ExportPreset.UHD60.next() != ExportPreset.HD60) throw new AssertionError("preset carousel wrap failed");
            if (ExportPreset.UHD60.width() != 3840 || ExportPreset.UHD60.height() != 2160)
                throw new AssertionError("4K preset dimensions incorrect");

            Path replays = Files.createDirectory(d.resolve("replays"));
            Files.writeString(replays.resolve("z.zip"), "older", StandardCharsets.UTF_8);
            Files.setLastModifiedTime(replays.resolve("z.zip"), java.nio.file.attribute.FileTime.fromMillis(10_000L));
            Files.writeString(replays.resolve("a.zip"), "newer", StandardCharsets.UTF_8);
            Files.setLastModifiedTime(replays.resolve("a.zip"), java.nio.file.attribute.FileTime.fromMillis(20_000L));
            Files.writeString(replays.resolve("ignore.txt"), "skip");
            var all = ReplayIndex.recent(replays, 10);
            if (all.size() != 2 || !all.getFirst().fileName().equals("a.zip")) throw new AssertionError("index ordering failed");
            if (ReplayIndex.recent(replays, 1).size() != 1) throw new AssertionError("limit failed");
            if (!ReplayIndex.recent(d.resolve("missing"), 10).isEmpty()) throw new AssertionError("missing folder failed");
            PerformanceSampler meter = new PerformanceSampler();
            meter.start();
            for (int i = 0; i < 100; i++) meter.tick(100, false);
            for (int i = 0; i < 100; i++) meter.tick(90, true);
            meter.stop();
            var m = meter.result();
            if (!m.comparable() || m.idleFps() != 100 || m.recordingFps() != 90 || m.relativeChangePercent() != -10)
                throw new AssertionError("FPS sampler failed");
            for (int[] viewport : new int[][]{{280,180},{320,240},{500,300},{683,384},{1920,1080}}) {
                var l = StudioLayout.forViewport(viewport[0], viewport[1]);
                if (l.x() < 0 || l.y() < 0 || l.width() > viewport[0] || l.height() > viewport[1]
                        || l.contentX() < l.x() || l.contentY() < l.y()
                        || l.contentX() + l.contentWidth() > l.x() + l.width()
                        || l.contentY() + l.contentHeight() > l.y() + l.height()) {
                    throw new AssertionError("Studio content out of bounds: " + l);
                }
            }
            for (int[] viewport : new int[][]{{220,120},{320,240},{426,240},{640,360},{854,480},{1920,1080}}) {
                var pause = PauseMenuLayout.forViewport(viewport[0], viewport[1]);
                if (pause.x()<0 || pause.y()<0 || pause.x()+pause.width()>viewport[0]
                        || pause.y()+pause.buttonHeight()>viewport[1])
                    throw new AssertionError("Pause buttons off-screen: " + pause);
            }
            System.out.println("PASS: persisted export presets, 120fps math, 4K dimensions, replay indexing, FPS math, responsive Studio layouts");
        } finally {
            try (var s = Files.walk(d)) {
                for (Path p : s.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }
}
