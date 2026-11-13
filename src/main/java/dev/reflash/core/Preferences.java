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
        showNotifications = Boolean.parseBoolean(p.getProperty("notifications", "true"));
        exportPreset = ExportPreset.fromId(p.getProperty("exportPreset", "QHD120"));
    }

    public void save() throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Properties p = new Properties();
        p.setProperty("autoRecord", Boolean.toString(autoRecord));
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

    public boolean autoRecord() { return autoRecord; }
    public boolean notifications() { return showNotifications; }
    public void setAutoRecord(boolean value) { autoRecord = value; }
    public void setNotifications(boolean value) { showNotifications = value; }
    public ExportPreset exportPreset() { return exportPreset; }
    public void setExportPreset(ExportPreset preset) { exportPreset = java.util.Objects.requireNonNull(preset); }
}
