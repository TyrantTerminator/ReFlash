package dev.reflash.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Fast, read-only listing of saved Momenton replay archives; no decompression. */
public final class ReplayIndex {
    public record Replay(String fileName, Path path, long byteSize, Instant modifiedAt) { }
    private ReplayIndex() { }

    public static List<Replay> recent(Path folder, int limit) throws IOException {
        if (limit <= 0 || !Files.isDirectory(folder)) return List.of();
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".momenton"))
                    .map(p -> {
                        try {
                            return new Replay(p.getFileName().toString(), p,
                                    Files.size(p), Files.getLastModifiedTime(p).toInstant());
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(Replay::modifiedAt).reversed())
                    .limit(limit).toList();
        }
    }
}
