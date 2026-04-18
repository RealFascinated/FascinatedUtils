package cc.fascinated.fascinatedutils.common;

import java.util.Locale;

public class SystemUtils {
    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).contains("win");
    }
}
