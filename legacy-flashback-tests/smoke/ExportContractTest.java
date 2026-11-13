import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.windows.StartExportWindow;
import dev.reflash.client.FlashbackExportBridge;
import dev.reflash.core.ExportPreset;
import java.util.Arrays;

public final class ExportContractTest {
    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }
    public static void main(String[] args) {
        check(ExportPreset.QHD120.width() == 2560 && ExportPreset.QHD120.height() == 1440,
                "1440p dimensions");
        check(ExportPreset.QHD120.fps() == 120, "120 FPS preset");
        check(ExportPreset.QHD120.frameCount(60) == 7200, "60 second frames");
        check(ExportPreset.QHD120.pixelsPerSecond() == 442368000L, "pixel throughput");
        check(ExportPreset.QHD120.estimatedVideoBytes(60, 40) == 300_000_000L, "40Mbps storage estimate");
        check(ExportPreset.QHD120.estimatedVideoBytes(0, 40) == 0, "zero duration");
        check(ExportPreset.QHD120.estimatedVideoBytes(60, 0) == 0, "zero bitrate");
        try { ExportPreset.QHD120.estimatedVideoBytes(-1, 10); throw new AssertionError("negative duration accepted"); }
        catch (IllegalArgumentException expected) { }
        Flashback.server = null;
        check(!FlashbackExportBridge.applyAndOpenNativeExport(ExportPreset.QHD120),
                "export rejected outside replay");
        check(StartExportWindow.calls == 0, "should not open without replay");
        Flashback.server = new com.moulberry.flashback.playback.ReplayServer();
        check(FlashbackExportBridge.applyAndOpenNativeExport(ExportPreset.QHD120), "1440p120 native export launch");
        check(Arrays.equals(Flashback.config.internalExport.resolution, new int[]{2560,1440}), "actual resolution set");
        check(Arrays.equals(Flashback.config.internalExport.framerate, new float[]{120f}), "actual framerate set");
        check(Arrays.equals(Flashback.config.forceDefaultExportSettings.resolution, new int[]{2560,1440}), "force-default resolution sync");
        check(Arrays.equals(Flashback.config.forceDefaultExportSettings.framerate, new float[]{120f}), "force-default framerate sync");
        check(StartExportWindow.calls == 1, "Flashback native window opened");
        StartExportWindow.shouldFail = true;
        check(!FlashbackExportBridge.applyAndOpenNativeExport(ExportPreset.HD60), "handles export dialog failure");
        check(Flashback.config.internalExport.resolution[0] == 2560 && Flashback.config.internalExport.framerate[0] == 120,
                "rollback after error");
        check(Flashback.config.forceDefaultExportSettings.resolution[0] == 2560 &&
            Flashback.config.forceDefaultExportSettings.framerate[0] == 120,
                "force defaults rollback after error");
        System.out.println("PASS: 1440p120 7200 frames/min; native export resolution/framerate + force-default sync; missing replay; rollback");
    }
}
