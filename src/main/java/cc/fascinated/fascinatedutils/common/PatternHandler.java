package cc.fascinated.fascinatedutils.common;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PatternHandler {
    public static PatternHandler INSTANCE = new PatternHandler();

    private final Map<String, Pattern> patternCache = new HashMap<>();

    public Pattern getPattern(String pattern) {
        String processedPattern = PlaceholderAPI.INSTANCE.process(pattern);
        String cacheKey = processedPattern != null ? processedPattern : pattern;
        return patternCache.computeIfAbsent(cacheKey, Pattern::compile);
    }

    public void clearPatterns() {
        patternCache.clear();
    }
}
