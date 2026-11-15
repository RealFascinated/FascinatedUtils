package cc.fascinated.fascinatedutils.systems.config;

import cc.fascinated.fascinatedutils.debug.perf.PerfMode;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AtomicFiles {
    public static final AtomicFiles INSTANCE = new AtomicFiles();

    /**
     * Writes UTF-8 text to a {@code .tmp} sibling and then moves it over the target, so readers never see a partial file.
     * Does nothing during a world perf scenario so the scenario never touches user files.
     *
     * @param target  the file to replace
     * @param content the text to write
     * @throws IOException if the file could not be written or moved into place
     */
    public void writeAtomically(Path target, String content) throws IOException {
        if (PerfMode.isWorldScenario()) {
            return;
        }
        Path temporary = prepareTemporary(target);
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        moveIntoPlace(temporary, target);
    }

    /**
     * Streams a JSON element as pretty-printed UTF-8 text to a {@code .tmp} sibling and then moves it over the target.
     * The bytes written are the same as {@code gson.toJson(json)} written with {@link #writeAtomically(Path, String)},
     * but the text is never built as one large String.
     *
     * @param target the file to replace
     * @param json   the JSON to write
     * @param gson   the Gson instance that formats the JSON
     * @throws IOException if the file could not be written or moved into place
     */
    public void writeAtomically(Path target, JsonElement json, Gson gson) throws IOException {
        if (PerfMode.isWorldScenario()) {
            return;
        }
        Path temporary = prepareTemporary(target);
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            gson.toJson(json, writer);
        } catch (JsonIOException exception) {
            if (exception.getCause() instanceof IOException cause) {
                throw cause;
            }
            throw exception;
        }
        moveIntoPlace(temporary, target);
    }

    private Path prepareTemporary(Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return target.resolveSibling(target.getFileName() + ".tmp");
    }

    private void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Sets an unreadable file aside as a {@code .corrupt} sibling so it is kept for recovery.
     *
     * @param file              the file that could not be read or parsed
     * @param copyInsteadOfMove true to leave the original in place, false to move it
     * @return the {@code .corrupt} file that was written
     * @throws IOException if the file could not be copied or moved
     */
    public Path quarantine(Path file, boolean copyInsteadOfMove) throws IOException {
        Path corruptPath = file.resolveSibling(file.getFileName() + ".corrupt");
        if (copyInsteadOfMove) {
            Files.copy(file, corruptPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(file, corruptPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return corruptPath;
    }
}
