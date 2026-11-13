package dev.reflash.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Run only when the export page opens or user requests a probe; never per game frame. */
public final class FfmpegCheck {
    private FfmpegCheck() { }
    public static String candidate(Path gameDir) {
        Path local = gameDir.resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe");
        if (Files.isRegularFile(local)) return local.toString();
        Path localFlat = gameDir.resolve("ffmpeg").resolve("ffmpeg.exe");
        if (Files.isRegularFile(localFlat)) return localFlat.toString();
        return "ffmpeg";
    }
    public static boolean available(Path gameDir) {
        Process process = null;
        try {
            process = new ProcessBuilder(candidate(gameDir), "-version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) { process.destroyForcibly(); return false; }
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
