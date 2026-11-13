package com.moulberry.flashback.editor.ui.windows;
import com.moulberry.flashback.Flashback;
public class StartExportWindow {
    public static int calls;
    public static boolean shouldFail;
    public static void open() {
        if (shouldFail) throw new IllegalStateException("Simulated exporter failure");
        if (Flashback.config.internalExport.resolution[0] < 16) throw new IllegalStateException("Invalid width");
        calls++;
    }
}
