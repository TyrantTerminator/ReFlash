package dev.reflash.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;

/**
 * Recording diagnostics, written only when a session changes state.
 * Does not capture world/server names, chat, microphone, or screen content.
 */
public final class RecordingSessionDiagnostics {
    private final Path file;
    private long startedAtNanos;
    private boolean active;
    private int lastTicks;
    private double lastWallSeconds;

    public RecordingSessionDiagnostics(Path file) { this.file = file; }
    public Path path() { return file; }

    public synchronized void started() {
        active = true;
        startedAtNanos = System.nanoTime();
        lastTicks = 0;
        lastWallSeconds = 0;
        append("START", 0, 0, "user action");
    }

    public synchronized void stopped(int gameTicks, boolean manual, boolean disconnected) {
        if (!active) return;
        active = false;
        lastTicks = Math.max(0,gameTicks);
        lastWallSeconds = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0;
        String reason = manual ? "MANUAL_STOP" : disconnected ? "DISCONNECT" : "UNEXPECTED_STOP";
        append(reason, lastTicks, lastWallSeconds, "game ticks compared with wall clock");
    }

    public synchronized void saveResult(int savedTicks, String result) {
        if (active) return;
        if (!result.equals("SAVED") && !result.equals("SHORTENED") && !result.equals("UNCONFIRMED"))
            throw new IllegalArgumentException("Invalid result");
        append(result, Math.max(0, savedTicks), lastWallSeconds,
                "recordedTicks=" + lastTicks + "; savedTicks=" + Math.max(0,savedTicks));
    }

    private void append(String event, int ticks, double wallSeconds, String detail) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            String line = Instant.now()+"\t"+event+"\t"+ticks+"\t"+
                    String.format(Locale.ROOT,"%.1f",wallSeconds)+"\t"+detail+"\n";
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Diagnostics must never interfere with replay recording or tick rate.
        }
    }
}
