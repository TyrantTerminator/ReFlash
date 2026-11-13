package dev.reflash.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offline video post-processing, not a native-resolution Vulkan renderer.
 * Rendering still happens through Momenton at the game-window size. This tool then
 * scales/pads that video and establishes a constant output FPS (duplicating frames
 * when needed). It cannot reconstruct detail or motion that wasn't captured.
 */
public final class VideoPostProcessor {
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final AtomicBoolean CANCELLED = new AtomicBoolean();
    private static volatile Process active;
    private static volatile String status = "Ready";
    private static volatile Path lastOutput;
    private VideoPostProcessor() { }

    public static boolean running() { return RUNNING.get(); }
    public static String status() { return status; }
    public static Path lastOutput() { return lastOutput; }

    public static Path latestExport(Path gameDir) throws IOException {
        Path folder = gameDir.resolve("momenton").resolve("exports");
        if (!Files.isDirectory(folder)) return null;
        try (var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> { String name=p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".mov")) &&
                                !name.contains("momenton-video-") && !name.contains("-reflash-"); })
                    .max(Comparator.comparingLong(p -> {
                        try { return Files.getLastModifiedTime(p).toMillis(); }
                        catch (IOException e) { return 0L; }
                    })).orElse(null);
        }
    }

    public static String filter(int width, int height, int fps) {
        if (width < 640 || height < 360 || width > 3840 || height > 2160 ||
                width%2 != 0 || height%2 != 0 || fps < 24 || fps > 240)
            throw new IllegalArgumentException("Maximum 3840x2160 at 240 FPS");
        return "scale="+width+":"+height+":flags=lanczos:force_original_aspect_ratio=decrease,"+
                "pad="+width+":"+height+":(ow-iw)/2:(oh-ih)/2,fps="+fps;
    }

    public static synchronized boolean startLatest(Path gameDir, Preferences preferences) {
        if (!RUNNING.compareAndSet(false, true)) return false;
        try {
            CANCELLED.set(false);
            Path input = latestExport(gameDir);
            if (input == null) {
                status = "No exported video yet. Render a replay in Momenton first.";
                RUNNING.set(false);
                return false;
            }
            String name = input.getFileName().toString().replaceFirst("(?i)\\.(mp4|mkv|mov)$", "");
            Path output = input.resolveSibling(name+"-ReFlash-"+preferences.outputWidth()+"x"+
                    preferences.outputHeight()+"-"+preferences.outputFps()+"fps.mp4");
            if (Files.exists(output)) {
                output = input.resolveSibling(name+"-ReFlash-"+System.currentTimeMillis()+".mp4");
            }
            final Path out = output;
            final String vf = filter(preferences.outputWidth(), preferences.outputHeight(), preferences.outputFps());
            status = "Enhancing latest export (software encoding)...";
            Thread thread = new Thread(() -> transcode(input, out, vf), "ReFlash-offline-video-export");
            thread.setDaemon(true);
            thread.start();
            return true;
        } catch (IOException | IllegalArgumentException e) {
            status = "Export setup failed: "+e.getMessage();
            RUNNING.set(false);
            return false;
        }
    }
    private static void transcode(Path input, Path out, String vf) {
        Process process = null;
        try {
            var cmd = new ProcessBuilder("ffmpeg", "-hide_banner", "-nostdin", "-y", "-i", input.toString(),
                    "-vf", vf, "-fps_mode", "cfr", "-c:v", "libx264", "-preset", "veryfast",
                    "-crf", "21", "-threads", "2", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "160k",
                    "-movflags", "+faststart", out.toString());
            process = cmd.redirectErrorStream(true).start();
            active = process;
            String lastError = "Unknown error";
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("frame=") || line.contains("time=")) {
                        int index = line.indexOf("time=");
                        if(index>=0)status="Rendering video  "+line.substring(index,Math.min(line.length(), index+18)).trim();
                    } else if (line.contains("Error") || line.contains("error") || line.contains("not found")) {
                        lastError = line.length()>150?line.substring(0,150):line;
                    }
                }
            }
            int code=process.waitFor();
            if(CANCELLED.get()) { status="Export cancelled"; }
            else if(code==0 && Files.isRegularFile(out) && Files.size(out)>0){
                lastOutput=out;
                status="Video ready: "+out.getFileName();
            } else {
                status="FFmpeg failed (code "+code+"): "+lastError;
            }
        } catch (IOException e) {
            status="Cannot run FFmpeg; install FFmpeg or add it to PATH. "+e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status="Export cancelled";
        } finally {
            active=null;
            RUNNING.set(false);
        }
    }
    public static void cancel() {
        Process p=active;
        if(p!=null && RUNNING.get()) {
            CANCELLED.set(true);
            p.destroyForcibly();
            status="Cancelling export...";
        }
    }
}
