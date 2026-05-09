package cc.fascinated.fascinatedutils.gui2.render;

import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class UiText {

    /**
     * A single styled text run: a plain string together with its rendering attributes.
     */
    private record Segment(String text, int colorArgb, boolean shadow, boolean bold, float scale) {

        int width(RenderFrame frame) {
            return widthOf(frame, text);
        }

        int widthOf(RenderFrame frame, String str) {
            return Math.round(frame.measureTextWidth(str, bold) * scale);
        }

        int height(RenderFrame frame) {
            return Math.round(frame.fontHeight() * scale);
        }

        void draw(RenderFrame frame, int positionX, int positionY) {
            draw(frame, text, positionX, positionY);
        }

        void draw(RenderFrame frame, String str, int positionX, int positionY) {
            if (str.isEmpty()) {
                return;
            }
            if (scale != 1.0f) {
                frame.pushTransform(positionX, positionY, scale, scale);
                frame.drawText(str, 0, 0, colorArgb, shadow, bold);
                frame.popTransform();
            } else {
                frame.drawText(str, positionX, positionY, colorArgb, shadow, bold);
            }
        }

        Segment withColor(int argb) {
            return new Segment(text, argb, shadow, bold, scale);
        }

        Segment withShadow(boolean shadow) {
            return new Segment(text, colorArgb, shadow, bold, scale);
        }

        Segment withBold(boolean bold) {
            return new Segment(text, colorArgb, shadow, bold, scale);
        }

        Segment withScale(float scale) {
            return new Segment(text, colorArgb, shadow, bold, scale);
        }
    }

    private record WordToken(String word, Segment segment) {}

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private final List<Segment> segments;

    private UiText(List<Segment> segments) {
        this.segments = Collections.unmodifiableList(segments);
    }

    /**
     * Creates a {@code UiText} for the given literal string with default styling (white, no
     * shadow, no bold, scale 1.0).
     *
     * @param text
     *         the literal text; {@code null} is treated as an empty string
     *
     * @return a new single-segment {@code UiText}
     */
    public static UiText of(String text) {
        return single(text != null ? text : "");
    }

    /**
     * Creates a {@code UiText} whose string is the resolved value of a translatable language key.
     *
     * @param key
     *         the translation key to look up
     *
     * @return a new single-segment {@code UiText}
     */
    public static UiText ofTranslatable(String key) {
        return of(Component.translatable(key).getString());
    }

    /**
     * Returns a copy with the root segment's color set to the given ARGB value.
     *
     * @param argb
     *         the ARGB color value
     *
     * @return a new {@code UiText} with the updated root color
     */
    public UiText color(int argb) {
        return mapRoot(root -> root.withColor(argb));
    }

    /**
     * Returns a copy with the root segment's color resolved from the current theme.
     *
     * @param resolver
     *         a function that picks a color from a {@link UiTheme}
     *
     * @return a new {@code UiText} with the resolved root color
     */
    public UiText color(Function<UiTheme, Integer> resolver) {
        return color(resolver.apply(UiThemeRepository.get()));
    }

    /**
     * Returns a copy with shadow enabled on the root segment.
     *
     * @return a new {@code UiText} with shadow on
     */
    public UiText shadow() {
        return shadow(true);
    }

    /**
     * Returns a copy with the root segment's shadow flag set to the given value.
     *
     * @param shadow
     *         {@code true} to enable shadow
     *
     * @return a new {@code UiText} with the updated shadow flag
     */
    public UiText shadow(boolean shadow) {
        return mapRoot(root -> root.withShadow(shadow));
    }

    /**
     * Returns a copy with bold enabled on the root segment.
     *
     * @return a new {@code UiText} with bold on
     */
    public UiText bold() {
        return bold(true);
    }

    /**
     * Returns a copy with the root segment's bold flag set to the given value.
     *
     * @param bold
     *         {@code true} to enable bold
     *
     * @return a new {@code UiText} with the updated bold flag
     */
    public UiText bold(boolean bold) {
        return mapRoot(root -> root.withBold(bold));
    }

    /**
     * Returns a copy with the root segment rendered at the given scale factor.
     *
     * <p>Scale is applied via {@link RenderFrame#pushTransform} at draw time so the segment
     * renders at the correct logical position regardless of scale.
     *
     * @param scale
     *         the uniform scale factor; {@code 1.0f} means no scaling
     *
     * @return a new {@code UiText} with the updated root scale
     */
    public UiText scale(float scale) {
        return mapRoot(root -> root.withScale(scale));
    }

    /**
     * Returns a new {@code UiText} with the segments of {@code other} appended after this
     * instance's segments.
     *
     * <p>Neither {@code this} nor {@code other} is mutated.
     *
     * @param other
     *         the text to append
     *
     * @return a new {@code UiText} containing all segments of both
     */
    public UiText append(UiText other) {
        List<Segment> combined = new ArrayList<>(segments.size() + other.segments.size());
        combined.addAll(segments);
        combined.addAll(other.segments);
        return new UiText(combined);
    }

    /**
     * Returns the total pixel width of all segments at their respective scales.
     *
     * @param frame
     *         the render frame used for font measurement
     *
     * @return the combined width in logical pixels
     */
    public int width(RenderFrame frame) {
        int total = 0;
        for (Segment segment : segments) {
            total += segment.width(frame);
        }
        return total;
    }

    /**
     * Returns the pixel height of the tallest segment, i.e. the line height at the largest scale.
     *
     * @param frame
     *         the render frame used for font measurement
     *
     * @return the maximum segment height in logical pixels
     */
    public int height(RenderFrame frame) {
        int max = 0;
        for (Segment segment : segments) {
            max = Math.max(max, segment.height(frame));
        }
        return max;
    }

    /**
     * Draws all segments left to right starting at the given position, advancing the horizontal
     * cursor by each segment's scaled width.
     *
     * @param frame
     *         the render frame to draw with
     * @param positionX
     *         the left edge of the first segment in logical pixels
     * @param positionY
     *         the top edge in logical pixels
     */
    public void draw(RenderFrame frame, int positionX, int positionY) {
        int cursorX = positionX;
        for (Segment segment : segments) {
            segment.draw(frame, cursorX, positionY);
            cursorX += segment.width(frame);
        }
    }

    /**
     * Draws this text with word-wrapping within the given maximum width, starting a new line
     * whenever a word would exceed it. Newline characters inside segment text force a line break.
     *
     * <p>When a line contains segments at different scales they are vertically centred relative
     * to the tallest segment on that line.
     *
     * @param frame
     *         the render frame to draw with
     * @param x
     *         left edge of the text block in logical pixels
     * @param y
     *         top edge of the first line in logical pixels
     * @param maxWidth
     *         maximum line width before wrapping, in logical pixels
     * @param lineSpacing
     *         extra vertical gap between lines, in logical pixels
     *
     * @return the total height used in logical pixels, not including a trailing gap
     */
    public int draw(RenderFrame frame, int x, int y, int maxWidth, int lineSpacing) {
        List<List<WordToken>> lines = flow(frame, maxWidth);
        if (lines.isEmpty()) {
            return 0;
        }
        int baseLineHeight = frame.fontHeight();
        int cursorY = y;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<WordToken> line = lines.get(lineIndex);
            int lineHeight = baseLineHeight;
            for (WordToken token : line) {
                lineHeight = Math.max(lineHeight, token.segment().height(frame));
            }
            int cursorX = x;
            Segment prevSegment = null;
            for (WordToken token : line) {
                if (prevSegment != null) {
                    int spaceWidth = prevSegment.widthOf(frame, " ");
                    prevSegment.draw(frame, " ", cursorX, cursorY + (lineHeight - prevSegment.height(frame)) / 2);
                    cursorX += spaceWidth;
                }
                token.segment().draw(frame, token.word(), cursorX, cursorY + (lineHeight - token.segment().height(frame)) / 2);
                cursorX += token.segment().widthOf(frame, token.word());
                prevSegment = token.segment();
            }
            cursorY += lineHeight;
            if (lineIndex < lines.size() - 1) {
                cursorY += lineSpacing;
            }
        }
        return cursorY - y;
    }

    /**
     * Measures the height this text would occupy when drawn with word-wrapping at the given
     * maximum width, without actually drawing anything.
     *
     * @param frame
     *         the render frame used for font measurement
     * @param maxWidth
     *         maximum line width before wrapping, in logical pixels
     * @param lineSpacing
     *         extra vertical gap between lines, in logical pixels
     *
     * @return the total height in logical pixels, not including a trailing gap
     */
    public int wrappedHeight(RenderFrame frame, int maxWidth, int lineSpacing) {
        List<List<WordToken>> lines = flow(frame, maxWidth);
        if (lines.isEmpty()) {
            return 0;
        }
        int baseLineHeight = frame.fontHeight();
        int total = 0;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            int lineHeight = baseLineHeight;
            for (WordToken token : lines.get(lineIndex)) {
                lineHeight = Math.max(lineHeight, token.segment().height(frame));
            }
            total += lineHeight;
            if (lineIndex < lines.size() - 1) {
                total += lineSpacing;
            }
        }
        return total;
    }

    private List<List<WordToken>> flow(RenderFrame frame, int maxWidth) {
        List<Object> tokens = new ArrayList<>();
        for (Segment seg : segments) {
            String[] paragraphs = seg.text().split("\\R", -1);
            for (int pi = 0; pi < paragraphs.length; pi++) {
                if (pi > 0) {
                    tokens.add(null); // forced line break
                }
                for (String word : paragraphs[pi].split(" +", -1)) {
                    if (!word.isEmpty()) {
                        tokens.add(new WordToken(word, seg));
                    }
                }
            }
        }

        List<List<WordToken>> lines = new ArrayList<>();
        List<WordToken> currentLine = new ArrayList<>();
        int currentWidth = 0;

        for (Object token : tokens) {
            if (token == null) {
                lines.add(currentLine);
                currentLine = new ArrayList<>();
                currentWidth = 0;
                continue;
            }
            WordToken wordToken = (WordToken) token;
            int wordWidth = wordToken.segment().widthOf(frame, wordToken.word());
            if (wordWidth > maxWidth) {
                // Word is too wide to fit on any line on its own — must be split.
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                    currentWidth = 0;
                }
                String word = wordToken.word();
                Segment seg = wordToken.segment();
                int start = 0;
                while (start < word.length()) {
                    int end = longestPrefixFits(seg, word, start, maxWidth, frame);
                    String chunk = word.substring(start, end);
                    start = end;
                    if (start < word.length()) {
                        // Not the last chunk — emit as its own line.
                        List<WordToken> chunkLine = new ArrayList<>();
                        chunkLine.add(new WordToken(chunk, seg));
                        lines.add(chunkLine);
                    } else {
                        // Last chunk — keep on currentLine so following words can join.
                        currentLine.add(new WordToken(chunk, seg));
                        currentWidth = seg.widthOf(frame, chunk);
                    }
                }
            } else {
                int spaceWidth = currentLine.isEmpty() ? 0 : currentLine.get(currentLine.size() - 1).segment().widthOf(frame, " ");
                int needed = spaceWidth + wordWidth;
                if (!currentLine.isEmpty() && currentWidth + needed > maxWidth) {
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                    currentLine.add(wordToken);
                    currentWidth = wordWidth;
                } else {
                    currentLine.add(wordToken);
                    currentWidth += needed;
                }
            }
        }

        if (!currentLine.isEmpty() || !lines.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    private static int longestPrefixFits(Segment seg, String word, int start, int maxWidth, RenderFrame frame) {
        int low = start + 1;
        int high = word.length();
        int best = start + 1; // always advance at least one character
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (seg.widthOf(frame, word.substring(start, mid)) <= maxWidth) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    private static UiText single(String text) {
        List<Segment> list = new ArrayList<>(1);
        list.add(new Segment(text, DEFAULT_COLOR, false, false, 1.0f));
        return new UiText(list);
    }

    private UiText mapRoot(UnaryOperator<Segment> transform) {
        if (segments.isEmpty()) {
            return this;
        }
        List<Segment> updated = new ArrayList<>(segments);
        updated.set(0, transform.apply(updated.get(0)));
        return new UiText(updated);
    }
}
