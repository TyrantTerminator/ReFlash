package dev.reflash.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Pure Java config: never does any work in the render loop. */
public final class Preferences {
    private final Path file;
    public enum HudPosition { TOP_LEFT, TOP_RIGHT, OFF;
        public HudPosition next() { return values()[(ordinal()+1)%values().length]; }
    }
    private HudPosition hudPosition = HudPosition.TOP_RIGHT;
    public enum HudSize { COMPACT, STANDARD;public HudSize next(){return values()[(ordinal()+1)%values().length];}}
    private HudSize hudSize = HudSize.COMPACT;
    private int hudOpacity = 75;
    private int outputWidth = 2560, outputHeight = 1440, outputFps = 120;
    private boolean autoRecord = false;
    private boolean showNotifications = true;
    private ExportPreset exportPreset = ExportPreset.QHD120;

    public Preferences(Path file) {
        this.file = file;
    }

    public void load() throws IOException {
        if (!Files.isRegularFile(file)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        }
        autoRecord = Boolean.parseBoolean(p.getProperty("autoRecord", "false"));
        try { hudPosition = HudPosition.valueOf(p.getProperty("recordHud", "TOP_RIGHT")); }
        catch (IllegalArgumentException ex) { hudPosition = HudPosition.TOP_RIGHT; }
        try {hudSize=HudSize.valueOf(p.getProperty("recordHudSize","COMPACT"));}
        catch(IllegalArgumentException e){hudSize=HudSize.COMPACT;}
        try{hudOpacity=Math.clamp(Integer.parseInt(p.getProperty("recordHudOpacity","75")),20,100);}
        catch(NumberFormatException e){hudOpacity=75;}
        try { setOutputPreset(Integer.parseInt(p.getProperty("outputWidth", "2560")),
                Integer.parseInt(p.getProperty("outputHeight", "1440")),
                Integer.parseInt(p.getProperty("outputFps", "120"))); }
        catch (NumberFormatException ex) { setOutputPreset(2560, 1440, 120); }
        showNotifications = Boolean.parseBoolean(p.getProperty("notifications", "true"));
        exportPreset = ExportPreset.fromId(p.getProperty("exportPreset", "QHD120"));
    }

    public void save() throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Properties p = new Properties();
        p.setProperty("autoRecord", Boolean.toString(autoRecord));
        p.setProperty("recordHud", hudPosition.name());
        p.setProperty("recordHudSize",hudSize.name());
        p.setProperty("recordHudOpacity",Integer.toString(hudOpacity));
        p.setProperty("outputWidth", Integer.toString(outputWidth));
        p.setProperty("outputHeight", Integer.toString(outputHeight));
        p.setProperty("outputFps", Integer.toString(outputFps));
        p.setProperty("notifications", Boolean.toString(showNotifications));
        p.setProperty("exportPreset", exportPreset.id());
        Path part = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(part)) {
            p.store(out, "ReFlash settings");
        }
        try {
            Files.move(part, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(part, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public HudPosition hudPosition() { return hudPosition; }
    public HudSize hudSize(){return hudSize;}
    public void setHudSize(HudSize size){hudSize=java.util.Objects.requireNonNull(size);}
    public int hudOpacity(){return hudOpacity;}
    public void setHudOpacity(int opacity){hudOpacity=Math.clamp(opacity,20,100);}
    public void setHudPosition(HudPosition p) { hudPosition = java.util.Objects.requireNonNull(p); }
    public int outputWidth() { return outputWidth; }
    public int outputHeight() { return outputHeight; }
    public int outputFps() { return outputFps; }
    public void setOutputPreset(int width, int height, int fps) {
        if(width<640||height<360||width>3840||height>2160||fps<24||fps>240||width%2!=0||height%2!=0)
            throw new IllegalArgumentException("Output must be even-sized, <=4K and <=240 FPS");
        outputWidth=width; outputHeight=height; outputFps=fps;
    }
    public String outputLabel() { return outputWidth+"x"+outputHeight+" @ "+outputFps+" FPS"; }
    public boolean autoRecord() { return autoRecord; }
    public boolean notifications() { return showNotifications; }
    public void setAutoRecord(boolean value) { autoRecord = value; }
    public void setNotifications(boolean value) { showNotifications = value; }
    public ExportPreset exportPreset() { return exportPreset; }
    public void setExportPreset(ExportPreset preset) { exportPreset = java.util.Objects.requireNonNull(preset); }
}
