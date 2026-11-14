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
    private boolean includeGameAudio = true;
    private int customCrf = -1;
    public enum RecordingMode { SCREEN_VIDEO, REPLAY_DATA;
        public RecordingMode next(){return this==SCREEN_VIDEO?REPLAY_DATA:SCREEN_VIDEO;}
    }
    private RecordingMode recordingMode = RecordingMode.SCREEN_VIDEO;
    private int captureFps = 30;
    private boolean captureMic = false, captureDesktop = false;
    private String screenMicDevice = "", screenDesktopDevice = "";
    private ExportPreset exportPreset = ExportPreset.QHD120;
    private UpscaleProfile upscaleProfile = UpscaleProfile.BALANCED;

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
        includeGameAudio = Boolean.parseBoolean(p.getProperty("includeGameAudio", "true"));
        try { setCustomCrf(Integer.parseInt(p.getProperty("customCrf", "-1"))); }
        catch (IllegalArgumentException e) { customCrf = -1; }
        try { recordingMode = RecordingMode.valueOf(p.getProperty("recordingMode", "SCREEN_VIDEO")); }
        catch (IllegalArgumentException e) { recordingMode = RecordingMode.SCREEN_VIDEO; }
        try { setCaptureFps(Integer.parseInt(p.getProperty("captureFps", "30"))); }
        catch (IllegalArgumentException e) { captureFps = 30; }
        captureMic = Boolean.parseBoolean(p.getProperty("captureMic", "false"));
        captureDesktop = Boolean.parseBoolean(p.getProperty("captureDesktop", "false"));
        screenMicDevice = p.getProperty("screenMicDevice", "");
        screenDesktopDevice = p.getProperty("screenDesktopDevice", "");
        exportPreset = ExportPreset.fromId(p.getProperty("exportPreset", "QHD120"));
        upscaleProfile = UpscaleProfile.byName(p.getProperty("upscaleProfile", "BALANCED"));
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
        p.setProperty("includeGameAudio", Boolean.toString(includeGameAudio));
        p.setProperty("customCrf", Integer.toString(customCrf));
        p.setProperty("recordingMode", recordingMode.name());
        p.setProperty("captureFps", Integer.toString(captureFps));
        p.setProperty("captureMic", Boolean.toString(captureMic));
        p.setProperty("captureDesktop", Boolean.toString(captureDesktop));
        p.setProperty("screenMicDevice", screenMicDevice);
        p.setProperty("screenDesktopDevice", screenDesktopDevice);
        p.setProperty("exportPreset", exportPreset.id());
        p.setProperty("upscaleProfile", upscaleProfile.name());
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
    public boolean includeGameAudio() { return includeGameAudio; }
    public void setIncludeGameAudio(boolean enabled) { includeGameAudio = enabled; }
    public RecordingMode recordingMode(){return recordingMode;}
    public void setRecordingMode(RecordingMode mode){recordingMode=java.util.Objects.requireNonNull(mode);}
    public int captureFps(){return captureFps;}
    public void setCaptureFps(int fps){
        if(fps<15||fps>240)throw new IllegalArgumentException("Capture FPS must be 15 to 240");
        captureFps=fps;
    }
    public boolean captureMicrophone(){return captureMic;}
    public void setCaptureMicrophone(boolean enabled){captureMic=enabled;}
    public boolean captureDesktopAudio(){return captureDesktop;}
    public void setCaptureDesktopAudio(boolean enabled){captureDesktop=enabled;}
    public String screenMicDevice(){return screenMicDevice;}
    public String screenDesktopDevice(){return screenDesktopDevice;}
    public void setScreenMicDevice(String name){screenMicDevice=limitDeviceName(name);}
    public void setScreenDesktopDevice(String name){screenDesktopDevice=limitDeviceName(name);}
    private static String limitDeviceName(String name){
        if(name==null)return "";
        name=name.trim();
        if(name.length()>256||name.indexOf('\n')>=0||name.indexOf('\r')>=0)
            throw new IllegalArgumentException("Invalid device name");
        return name;
    }
    public int qualityCrf() { return customCrf < 0 ? upscaleProfile.crf() : customCrf; }
    public void setCustomCrf(int crf) {
        if (crf != -1 && (crf < 14 || crf > 36))
            throw new IllegalArgumentException("Quality must be CRF 14 to 36");
        customCrf = crf;
    }
    public void setAutoRecord(boolean value) { autoRecord = value; }
    public void setNotifications(boolean value) { showNotifications = value; }
    public UpscaleProfile upscaleProfile(){return upscaleProfile;}
    public void setUpscaleProfile(UpscaleProfile profile){
        upscaleProfile=java.util.Objects.requireNonNull(profile);
        customCrf = -1; // explicitly choosing a preset restores its advertised quality
    }
    public ExportPreset exportPreset() { return exportPreset; }
    public void setExportPreset(ExportPreset preset) { exportPreset = java.util.Objects.requireNonNull(preset); }
}
