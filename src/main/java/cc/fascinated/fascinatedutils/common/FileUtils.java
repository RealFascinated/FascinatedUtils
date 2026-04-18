package cc.fascinated.fascinatedutils.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    public static void ensureDirectoryExists(Path directoryPath) throws IOException {
        Files.createDirectories(directoryPath);
    }
}
