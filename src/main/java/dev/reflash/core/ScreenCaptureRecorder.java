package dev.reflash.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Optional Windows desktop video recorder (FFmpeg gdigrab + selected DirectShow INPUTS).
 * Explicit user opt-in. Never uses replay-state capture and never starts in the background.
 * This is a compatibility path, NOT an OBS-grade Vulkan texture/hardware encoder.
 */
public final class ScreenCaptureRecorder {
    private ScreenCaptureRecorder() { }
    private static volatile Process recording;
    private static volatile String status = "Screen recording idle";
    private static volatile Path output;
    private static volatile long startedNanos;
    private static volatile boolean stopping;
    private static final DateTimeFormatter NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static synchronized boolean isRecording() {
        return recording != null && recording.isAlive();
    }
    public static String status() { return status; }
    public static Path lastOutput() { return output; }
    public static int recordingTicksApprox() {
        return isRecording() ? (int)Math.min(Integer.MAX_VALUE,
                Math.max(0, (System.nanoTime() - startedNanos) / 50_000_000L)) : 0;
    }

    public static synchronized boolean start(Path gameDir, Preferences settings) {
        if (isRecording() || stopping) { status = "Stop the active recording first"; return false; }
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            status = "Screen Video recording is currently Windows-only"; return false;
        }
        if (!FfmpegCheck.available(gameDir)) {
            status = "FFmpeg not found; install it or place ffmpeg.exe in Minecraft/ffmpeg/bin"; return false;
        }
        String mic = settings.captureMicrophone() ? settings.screenMicDevice() : "";
        String desktop = settings.captureDesktopAudio() ? settings.screenDesktopDevice() : "";
        if (settings.captureMicrophone() && mic.isBlank()) {
            status = "Choose a microphone under Settings > Audio, or switch Mic OFF"; return false;
        }
        if (settings.captureDesktopAudio() && desktop.isBlank()) {
            status = "Choose a Stereo Mix/loopback device, or switch Desktop audio OFF"; return false;
        }
        if (!desktop.isBlank() && desktop.equals(mic)) {
            status = "Mic and Desktop audio must use different capture devices"; return false;
        }
        try {
            Path dir = VideoExportLibrary.folder(gameDir);
            Files.createDirectories(dir);
            String stem = "ReFlash-screen-" + NAME.format(LocalDateTime.now());
            Path target = dir.resolve(stem + ".mkv");
            for (int i = 2; Files.exists(target) && i < 1000; i++) target = dir.resolve(stem + "-" + i + ".mkv");
            if (Files.exists(target)) { status = "Could not choose a unique video filename"; return false; }
            List<String> command = command(FfmpegCheck.candidate(gameDir), target, settings);
            Path log = dir.resolve("recorder-last.log");
            final Path finalTarget = target;
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (process.waitFor(250, TimeUnit.MILLISECONDS)) {
                status = "FFmpeg exited immediately (code " + process.exitValue() + "); see recorder-last.log";
                return false;
            }
            recording = process;
            output = target;
            startedNanos = System.nanoTime();
            stopping = false;
            status = "Recording screen to " + target.getFileName();
            Thread observer = new Thread(() -> {
                try {
                    int exit = process.waitFor();
                    synchronized (ScreenCaptureRecorder.class) {
                        if (recording == process) {
                            recording = null;
                            status = exit == 0 ? "Saved video: " + finalTarget.getFileName()
                                    : "Screen recording exited (" + exit + "); see recorder-last.log";
                        }
                        stopping = false;
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "ReFlash-ffmpeg-watch");
            observer.setDaemon(true);
            observer.start();
            return true;
        } catch (IOException e) {
            status = "Could not start screen recorder: " + e.getClass().getSimpleName();
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = "Recorder start interrupted";
            return false;
        }
    }

    /** Nonblocking UI/game-thread stop. FFmpeg finalizes its MKV using its regular q command. */
    public static synchronized boolean stop() {
        Process process = recording;
        if (process == null || !process.isAlive() || stopping) return false;
        stopping = true;
        status = "Finishing screen video...";
        Thread finisher = new Thread(() -> {
            try {
                try (OutputStream in = process.getOutputStream()) {
                    in.write("q\n".getBytes(StandardCharsets.US_ASCII));
                    in.flush();
                }
                if (!process.waitFor(15, TimeUnit.SECONDS)) {
                    process.destroy();
                    if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly();
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                process.destroy();
            }
        }, "ReFlash-ffmpeg-stop");
        finisher.setDaemon(true);
        finisher.start();
        return true;
    }

    /** Public for deterministic unit tests. Never feed these arguments to a shell. */
    public static List<String> command(String ffmpeg, Path target, Preferences settings) {
        List<String> args = new ArrayList<>(List.of(ffmpeg,
                "-hide_banner", "-y", "-loglevel", "warning", "-thread_queue_size", "512",
                "-f", "gdigrab", "-draw_mouse", "0", "-framerate", Integer.toString(settings.captureFps()),
                "-i", "desktop"));
        int audioCount = 0;
        if (settings.captureMicrophone()) {
            args.addAll(List.of("-thread_queue_size", "512", "-f", "dshow", "-i", "audio=" + settings.screenMicDevice()));
            audioCount++;
        }
        if (settings.captureDesktopAudio()) {
            args.addAll(List.of("-thread_queue_size", "512", "-f", "dshow", "-i", "audio=" + settings.screenDesktopDevice()));
            audioCount++;
        }
        args.addAll(List.of("-map", "0:v:0"));
        if (audioCount == 2) {
            args.addAll(List.of("-filter_complex", "[1:a:0][2:a:0]amix=inputs=2:duration=longest:dropout_transition=0[a]",
                    "-map", "[a]"));
        } else if (audioCount == 1) args.addAll(List.of("-map", "1:a:0"));
        else args.add("-an");
        args.addAll(List.of("-c:v", "libx264", "-preset", "ultrafast", "-crf",
                Integer.toString(settings.qualityCrf()), "-pix_fmt", "yuv420p"));
        if (audioCount != 0) args.addAll(List.of("-c:a", "aac", "-b:a", "160k"));
        args.addAll(List.of("-f", "matroska", target.toAbsolutePath().toString()));
        return List.copyOf(args);
    }
}
