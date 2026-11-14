package dev.reflash.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Enumerates Windows DirectShow capture INPUTS with FFmpeg, never touches the audio devices
 * until the user clicks Scan. Headphone playback endpoints are not capture devices unless
 * the driver exposes Stereo Mix/loopback, or the user installs a virtual capture device.
 */
public final class DirectShowAudioDevices {
    private DirectShowAudioDevices() { }
    private static volatile List<String> inputs = List.of();
    private static volatile List<String> loopbacks = List.of();
    private static volatile String status = "Click Scan audio devices";
    private static volatile boolean scanning;

    public static List<String> microphones() { return inputs; }
    public static List<String> desktopLoopbacks() { return loopbacks; }
    public static String status() { return status; }
    public static boolean scanning() { return scanning; }

    public static synchronized boolean scanAsync(Path gameDir) {
        if (scanning) return false;
        scanning = true;
        status = "Scanning Windows recording sources...";
        Thread worker = new Thread(() -> {
            try {
                if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
                    status = "Live desktop capture currently requires Windows";
                    return;
                }
                Process process = new ProcessBuilder(FfmpegCheck.candidate(gameDir),
                        "-hide_banner", "-list_devices", "true", "-f", "dshow", "-i", "dummy")
                        .redirectErrorStream(true).start();
                // DirectShow -list_devices deliberately exits non-zero. Parse stdout regardless.
                try (var stream = process.getInputStream()) {
                    if (!process.waitFor(8, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                        status = "FFmpeg audio enumeration timed out";
                        return;
                    }
                    String output = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    List<String> found = parseAudioDevices(output);
                    inputs = found;
                    loopbacks = found.stream().filter(DirectShowAudioDevices::looksLikeLoopback).toList();
                    status = found.isEmpty() ? "No DirectShow audio devices found; check FFmpeg and Windows permissions"
                            : "Found " + found.size() + " inputs, " + loopbacks.size() + " likely desktop loopback sources";
                }
            } catch (IOException ex) {
                status = "Could not run FFmpeg; install ffmpeg.exe or use the bundled encoder";
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                status = "Audio scan interrupted";
            } finally {
                scanning = false;
            }
        }, "ReFlash-audio-device-scan");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    public static List<String> parseAudioDevices(String output) {
        if (output == null || output.isEmpty()) return List.of();
        boolean audioSection = false;
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String line : output.split("\\R")) {
            if (line.contains("DirectShow video devices")) audioSection = false;
            if (line.contains("DirectShow audio devices")) audioSection = true;
            if (!audioSection || !line.contains("(audio)") || line.contains("Alternative name")) continue;
            int first = line.indexOf('"'), second = first < 0 ? -1 : line.indexOf('"', first + 1);
            if (first >= 0 && second > first + 1) names.add(line.substring(first + 1, second));
        }
        return List.copyOf(names);
    }

    public static boolean looksLikeLoopback(String device) {
        String text = device.toLowerCase(Locale.ROOT);
        return text.contains("stereo mix") || text.contains("loopback") ||
                text.contains("what u hear") || text.contains("waveout mix") ||
                text.contains("virtual-audio-capturer") || text.contains("virtual cable") ||
                text.contains("voicemeeter output") || text.contains("mixed output");
    }

    public static String cycle(List<String> options, String current) {
        if (options.isEmpty()) return "";
        int at = options.indexOf(current);
        return options.get((at + 1) % options.size());
    }
}
