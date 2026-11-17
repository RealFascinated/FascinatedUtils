package cc.fascinated.fascinatedutils.gui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Soft-wraps plain text to a pixel width. Results are cached per (text, width, bold), so an instance must always be
 * used with the same measuring function; call {@link #clearCache()} when that function's behaviour changes (for
 * example after a font or UI scale change).
 */
public class TextLineLayout {
    private static final int CACHE_CAPACITY = 256;

    private static final String ELLIPSIS = "…";

    private record CacheKey(String text, float maxWidth, boolean bold, int maxLines) {}

    private final Map<CacheKey, List<String>> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, List<String>> eldest) {
            return size() > CACHE_CAPACITY;
        }
    };

    /**
     * Drops every cached result.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Soft-wraps plain text to the width. Whitespace inside a line is preserved, whitespace at a wrap point is
     * dropped, words wider than the width are split by code point, and each input newline starts a new line (a
     * trailing newline yields a trailing empty line).
     *
     * @param text         the text to wrap; null or empty yields no lines
     * @param maxWidth     the available width in pixels; zero or less disables width wrapping
     * @param bold         whether the text is measured bold; part of the cache key
     * @param measureWidth measures the width in pixels of a string
     * @return the wrapped lines, unmodifiable
     */
    public List<String> wrapLines(String text, float maxWidth, boolean bold, ToIntFunction<String> measureWidth) {
        return wrapLines(text, maxWidth, bold, Integer.MAX_VALUE, measureWidth);
    }

    /**
     * Soft-wraps plain text like {@link #wrapLines(String, float, boolean, ToIntFunction)} and limits the result to a
     * number of lines. When lines are cut, the last kept line ends in an ellipsis that fits the width.
     *
     * @param text         the text to wrap; null or empty yields no lines
     * @param maxWidth     the available width in pixels; zero or less disables width wrapping
     * @param bold         whether the text is measured bold; part of the cache key
     * @param maxLines     the most lines to return; at least one
     * @param measureWidth measures the width in pixels of a string
     * @return the wrapped lines, unmodifiable
     */
    public List<String> wrapLines(String text, float maxWidth, boolean bold, int maxLines, ToIntFunction<String> measureWidth) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        int lineLimit = Math.max(1, maxLines);
        CacheKey key = new CacheKey(text, maxWidth, bold, lineLimit);
        List<String> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        List<String> lines = new ArrayList<>();
        int paragraphStart = 0;
        while (true) {
            int paragraphEnd = text.indexOf('\n', paragraphStart);
            if (paragraphEnd < 0) {
                wrapParagraph(text, paragraphStart, text.length(), maxWidth, measureWidth, lines);
                break;
            }
            wrapParagraph(text, paragraphStart, paragraphEnd, maxWidth, measureWidth, lines);
            paragraphStart = paragraphEnd + 1;
        }
        if (lines.size() > lineLimit) {
            List<String> kept = new ArrayList<>(lines.subList(0, lineLimit));
            kept.set(lineLimit - 1, ellipsize(kept.get(lineLimit - 1), maxWidth, measureWidth));
            lines = kept;
        }
        List<String> result = List.copyOf(lines);
        cache.put(key, result);
        return result;
    }

    /**
     * Appends an ellipsis to a line, trimming the line's tail until the result fits the width.
     */
    private static String ellipsize(String line, float maxWidth, ToIntFunction<String> measureWidth) {
        String kept = line;
        while (!kept.isEmpty() && maxWidth > 0f && measureWidth.applyAsInt(kept + ELLIPSIS) > maxWidth) {
            int cut = kept.offsetByCodePoints(kept.length(), -1);
            kept = kept.substring(0, cut);
        }
        return kept + ELLIPSIS;
    }

    private static void wrapParagraph(String text, int start, int end, float maxWidth, ToIntFunction<String> measureWidth, List<String> output) {
        if (start == end) {
            output.add("");
            return;
        }
        boolean unlimited = maxWidth <= 0f;
        StringBuilder line = new StringBuilder();
        int lineWidth = 0;
        String pendingSpace = "";
        int pendingSpaceWidth = 0;
        boolean atParagraphStart = true;
        int cursor = start;
        while (cursor < end) {
            boolean whitespace = isWhitespace(text.charAt(cursor));
            int tokenEnd = cursor;
            while (tokenEnd < end && isWhitespace(text.charAt(tokenEnd)) == whitespace) {
                tokenEnd++;
            }
            String token = text.substring(cursor, tokenEnd);
            cursor = tokenEnd;
            int tokenWidth = measureWidth.applyAsInt(token);
            if (whitespace) {
                if (atParagraphStart) {
                    line.append(token);
                    lineWidth += tokenWidth;
                }
                else if (!line.isEmpty()) {
                    pendingSpace = token;
                    pendingSpaceWidth = tokenWidth;
                }
                continue;
            }
            atParagraphStart = false;
            if (unlimited || line.isEmpty() && tokenWidth <= maxWidth) {
                line.append(token);
                lineWidth += tokenWidth;
            }
            else if (!line.isEmpty() && lineWidth + pendingSpaceWidth + tokenWidth <= maxWidth) {
                line.append(pendingSpace).append(token);
                lineWidth += pendingSpaceWidth + tokenWidth;
            }
            else {
                if (!line.isEmpty()) {
                    output.add(line.toString());
                    line.setLength(0);
                    lineWidth = 0;
                }
                if (tokenWidth <= maxWidth) {
                    line.append(token);
                    lineWidth = tokenWidth;
                }
                else {
                    lineWidth = breakLongWord(token, maxWidth, measureWidth, output, line);
                }
            }
            pendingSpace = "";
            pendingSpaceWidth = 0;
        }
        if (!line.isEmpty()) {
            output.add(line.toString());
        }
    }

    /**
     * Splits a word wider than the width into code-point-aligned pieces. Every piece but the last becomes its own line;
     * the last piece is left in the line builder so following words can continue after it.
     *
     * @return the measured width of the piece left in the line builder
     */
    private static int breakLongWord(String word, float maxWidth, ToIntFunction<String> measureWidth, List<String> output, StringBuilder line) {
        int start = 0;
        while (true) {
            int pieceEnd = longestPrefixFits(word, start, maxWidth, measureWidth);
            if (pieceEnd >= word.length()) {
                String lastPiece = word.substring(start);
                line.append(lastPiece);
                return measureWidth.applyAsInt(lastPiece);
            }
            output.add(word.substring(start, pieceEnd));
            start = pieceEnd;
        }
    }

    /**
     * Finds the longest end index after {@code start} whose substring still fits, always advancing by at least one code
     * point and never splitting a surrogate pair.
     */
    private static int longestPrefixFits(String text, int start, float maxWidth, ToIntFunction<String> measureWidth) {
        int minimumEnd = start + Character.charCount(text.codePointAt(start));
        int low = minimumEnd;
        int high = text.length();
        int best = minimumEnd;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int alignedEnd = alignDown(text, middle);
            if (measureWidth.applyAsInt(text.substring(start, alignedEnd)) <= maxWidth) {
                best = alignedEnd;
                low = middle + 1;
            }
            else {
                high = middle - 1;
            }
        }
        return best;
    }

    private static int alignDown(String text, int index) {
        if (index > 0 && index < text.length() && Character.isLowSurrogate(text.charAt(index)) && Character.isHighSurrogate(text.charAt(index - 1))) {
            return index - 1;
        }
        return index;
    }

    private static boolean isWhitespace(char character) {
        return Character.isWhitespace(character);
    }
}
