package dev.reflash.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/** Small, dependency-free project editor for Momenton's public .momenton JSON format. */
public final class ReplayProject {
    public record Clip(int startTick, int endTick) {
        public Clip {
            if (startTick < 0 || endTick <= startTick) throw new IllegalArgumentException("Invalid clip");
        }
        public int length() { return endTick - startTick; }
    }
    private static final Pattern DURATION = Pattern.compile("\\\"durationTicks\\\"\\s*:\\s*(\\d+)");
    private static final Pattern MIC = Pattern.compile("\\\"micStartTick\\\"\\s*:\\s*([0-9.eE+-]+)");
    private static final Pattern CLIP = Pattern.compile("\\{\\s*\\\"startTick\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"endTick\\\"\\s*:\\s*(\\d+)\\s*\\}");
    private final Path replay;
    private final int sourceTicks;
    private final boolean microphoneTrack;
    private final List<Clip> clips;

    private ReplayProject(Path replay, int sourceTicks, boolean microphoneTrack, List<Clip> clips) {
        this.replay = replay;
        this.sourceTicks = sourceTicks;
        this.microphoneTrack = microphoneTrack;
        this.clips = new ArrayList<>(clips);
    }

    public static Path projectPath(Path replay) {
        return replay.resolveSibling(replay.getFileName().toString() + ".json");
    }

    public static ReplayProject open(Path replay) throws IOException {
        if (!Files.isRegularFile(replay) || !replay.getFileName().toString().endsWith(".momenton"))
            throw new IOException("Not a Momenton replay");
        String metadata;
        try (ZipFile zip = new ZipFile(replay.toFile())) {
            var entry = zip.getEntry("metadata.json");
            if (entry == null) throw new IOException("Replay metadata is missing");
            try (var in = zip.getInputStream(entry)) { metadata = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        }
        Matcher duration = DURATION.matcher(metadata);
        if (!duration.find()) throw new IOException("Replay duration is missing");
        int ticks;
        try { ticks = Integer.parseInt(duration.group(1)); }
        catch (NumberFormatException ex) { throw new IOException("Invalid replay duration", ex); }
        if (ticks <= 0) throw new IOException("Replay has no playable frames");
        List<Clip> cuts = new ArrayList<>();
        Path project = projectPath(replay);
        if (Files.isRegularFile(project)) {
            String existing = Files.readString(project);
            Matcher match = CLIP.matcher(existing);
            while (match.find()) {
                int start = Integer.parseInt(match.group(1)), end = Integer.parseInt(match.group(2));
                if (end <= ticks && start >= 0 && start < end) cuts.add(new Clip(start, end));
            }
        }
        if (cuts.isEmpty()) cuts.add(new Clip(0, ticks));
        return new ReplayProject(replay, ticks, MIC.matcher(metadata).find(), cuts);
    }

    public int sourceTicks() { return sourceTicks; }
    public boolean hasMicrophoneTrack() { return microphoneTrack; }
    public List<Clip> clips() { return List.copyOf(clips); }
    public int lengthTicks() { return clips.stream().mapToInt(Clip::length).sum(); }

    public void trimStart(int index, int tick) {
        Clip c = clips.get(index);
        clips.set(index, new Clip(Math.clamp(tick, 0, c.endTick() - 1), c.endTick()));
    }
    public void trimEnd(int index, int tick) {
        Clip c = clips.get(index);
        clips.set(index, new Clip(c.startTick(), Math.clamp(tick, c.startTick() + 1, sourceTicks)));
    }
    public boolean split(int index) {
        Clip c = clips.get(index);
        int midpoint = c.startTick() + c.length() / 2;
        if (midpoint <= c.startTick() || midpoint >= c.endTick()) return false;
        clips.set(index, new Clip(c.startTick(), midpoint));
        clips.add(index+1, new Clip(midpoint, c.endTick()));
        return true;
    }
    public boolean delete(int index) {
        if (clips.size() <= 1) return false;
        clips.remove(index);
        return true;
    }

    /** Saves the exact JSON shape consumed by Momenton's Project/Timeline records. */
    public void save() throws IOException {
        Path file = projectPath(replay);
        if (Files.isRegularFile(file)) {
            Path backup = file.resolveSibling(file.getFileName() + ".reflash-backup");
            if (!Files.exists(backup)) Files.copy(file, backup);
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"replay\": \"").append(escape(replay.getFileName().toString()))
            .append("\",\n  \"timeline\": {\n    \"sourceTicks\": ").append(sourceTicks)
            .append(",\n    \"clips\": [\n");
        for (int i = 0; i < clips.size(); i++) {
            Clip c = clips.get(i);
            json.append("      {\"startTick\": ").append(c.startTick())
                .append(", \"endTick\": ").append(c.endTick()).append("}");
            if (i < clips.size()-1) json.append(',');
            json.append('\n');
        }
        json.append("    ]\n  }\n}\n");
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, json.toString(), StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
