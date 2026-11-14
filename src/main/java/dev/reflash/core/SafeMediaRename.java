package dev.reflash.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Safely rename an explicitly selected recording or output without changing its extension. */
public final class SafeMediaRename {
    private SafeMediaRename() { }

    public static String suggestedName(Path selected) {
        if (selected == null) return "";
        String s = selected.getFileName().toString();
        int dot = s.lastIndexOf('.');
        return dot <= 0 ? s : s.substring(0, dot);
    }

    public static Path rename(Path folder, Path selected, String requested, boolean replay) throws IOException {
        if (folder == null || selected == null || requested == null)
            throw new IOException("Select a saved recording first");
        Path home = folder.toRealPath();
        if (!Files.isRegularFile(selected) || Files.isSymbolicLink(selected) ||
                !selected.toRealPath().getParent().equals(home))
            throw new IOException("File must be inside the ReFlash recordings or exports folder");
        String extension = extension(selected);
        if (replay && !extension.equals(".reflash"))
            throw new IOException("A recording must have the .reflash extension");
        if (!replay && !(extension.equals(".mp4") || extension.equals(".mkv") || extension.equals(".mov")))
            throw new IOException("Select an MP4, MKV, or MOV file");
        String name = requested.strip();
        // Windows-safe names. Never trust a user-provided path or extension.
        if (name.isEmpty() || name.length() > 90 || name.equals(".") || name.equals("..") ||
                name.endsWith(".") || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*") ||
                name.toUpperCase(Locale.ROOT).matches("(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?"))
            throw new IOException("Invalid name. Use a short filename without / \\ : * ? or quotes");
        Path target = home.resolve(name + extension);
        if (selected.equals(target) || selected.toAbsolutePath().normalize().equals(target)) return selected;
        if (Files.exists(target)) throw new IOException("A file with that name already exists");

        Path project = selected.resolveSibling(selected.getFileName().toString() + ".json");
        Path renamedProject = target.resolveSibling(target.getFileName().toString() + ".json");
        boolean sidecar = replay && Files.isRegularFile(project);
        if (sidecar && Files.exists(renamedProject)) throw new IOException("A matching edit project already exists");
        Files.move(selected, target);
        if (sidecar) {
            try {
                Files.move(project, renamedProject);
            } catch (IOException error) {
                try { Files.move(target, selected); } catch (IOException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        }
        return target;
    }

    private static String extension(Path file) {
        String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int i = n.lastIndexOf('.');
        return i >= 0 ? n.substring(i) : "";
    }
}
