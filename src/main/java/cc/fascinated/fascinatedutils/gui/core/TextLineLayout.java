package cc.fascinated.fascinatedutils.gui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public class TextLineLayout {

    /**
     * Soft-wrap plain text to the width (word breaks preferred; long tokens split). Input newlines start a new
     * paragraph.
     */
    public static List<String> wrapLines(String text, float maxWidth, ToIntFunction<String> measureWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0f) {
            return lines;
        }
        String[] paragraphs = text.split("\n", -1);
        for (int paragraphIndex = 0; paragraphIndex < paragraphs.length; paragraphIndex++) {
            String paragraph = paragraphs[paragraphIndex];
            if (paragraph.isEmpty()) {
                if (paragraphIndex < paragraphs.length - 1) {
                    lines.add("");
                }
                continue;
            }
            wrapParagraphWords(paragraph, maxWidth, measureWidth, lines);
        }
        return lines;
    }

    private static void wrapParagraphWords(String paragraph, float maxWidth, ToIntFunction<String> measureWidth, List<String> targetLines) {
        String[] words = paragraph.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (measureWidth.applyAsInt(word) <= maxWidth) {
                appendWordToLine(word, maxWidth, measureWidth, targetLines, line);
            }
            else {
                if (!line.isEmpty()) {
                    targetLines.add(line.toString());
                    line.setLength(0);
                }
                breakLongWord(word, maxWidth, measureWidth, targetLines, line);
            }
        }
        if (!line.isEmpty()) {
            targetLines.add(line.toString());
        }
    }

    private static void appendWordToLine(String word, float maxWidth, ToIntFunction<String> measureWidth, List<String> targetLines, StringBuilder line) {
        if (line.isEmpty()) {
            line.append(word);
            return;
        }
        String candidate = line + " " + word;
        if (measureWidth.applyAsInt(candidate) <= maxWidth) {
            line.append(' ').append(word);
        }
        else {
            targetLines.add(line.toString());
            line.setLength(0);
            line.append(word);
        }
    }

    private static void breakLongWord(String word, float maxWidth, ToIntFunction<String> measureWidth, List<String> targetLines, StringBuilder line) {
        if (!line.isEmpty()) {
            targetLines.add(line.toString());
            line.setLength(0);
        }
        int start = 0;
        while (start < word.length()) {
            int bestEnd = longestPrefixFits(word, start, maxWidth, measureWidth);
            targetLines.add(word.substring(start, bestEnd));
            start = bestEnd;
        }
    }

    /**
     * Finds the longest end index strictly after {@code start} whose substring still fits the width budget.
     *
     * @param text         full source string
     * @param start        first code unit index in {@code text}
     * @param maxWidth     maximum measured width for the substring
     * @param measureWidth measures plain segment width in pixels
     * @return exclusive end index for {@code text.substring(start, end)}, at least {@code start + 1} when possible
     */
    private static int longestPrefixFits(String text, int start, float maxWidth, ToIntFunction<String> measureWidth) {
        int low = start + 1;
        int high = text.length();
        int best = start + 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            String sub = text.substring(start, mid);
            if (measureWidth.applyAsInt(sub) <= maxWidth) {
                best = mid;
                low = mid + 1;
            }
            else {
                high = mid - 1;
            }
        }
        return Math.min(text.length(), Math.max(start + 1, best));
    }
}

