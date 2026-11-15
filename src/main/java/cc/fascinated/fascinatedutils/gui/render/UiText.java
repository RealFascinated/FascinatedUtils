package cc.fascinated.fascinatedutils.gui.render;

import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class UiText {

    /**
     * A single styled text run: a plain string together with its rendering attributes.
     */
    private record Segment(String text, int colorArgb, boolean shadow, boolean bold, float scale,
                           Integer hoverColorArgb, Runnable onClick) {

        int width(AlumiteGuiRenderer frame) {
            return widthOf(frame, text);
        }

        int widthOf(AlumiteGuiRenderer frame, String str) {
            return Math.round(frame.measureTextWidth(str, bold) * scale);
        }

        int height(AlumiteGuiRenderer frame) {
            return Math.round(frame.fontHeight() * scale);
        }

        void draw(AlumiteGuiRenderer frame, int positionX, int positionY) {
            draw(frame, text, positionX, positionY);
        }

        void draw(AlumiteGuiRenderer frame, String str, int positionX, int positionY) {
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

        void drawInteractive(AlumiteGuiRenderer frame, int positionX, int positionY, int w, int h) {
            drawInteractive(frame, text, positionX, positionY, w, h);
        }

        void drawInteractive(AlumiteGuiRenderer frame, String str, int positionX, int positionY, int w, int h) {
            boolean hovered = hoverColorArgb != null && isPointerOver(frame, positionX, positionY, w, h);
            Segment effective = hovered ? withColor(hoverColorArgb) : this;
            effective.draw(frame, str, positionX, positionY);
            if (onClick != null) {
                frame.addClickRegion(positionX, positionY, w, h, onClick);
            }
        }

        boolean isInteractive() {
            return hoverColorArgb != null || onClick != null;
        }

        Segment withColor(int argb) {
            return new Segment(text, argb, shadow, bold, scale, hoverColorArgb, onClick);
        }

        Segment withShadow(boolean shadow) {
            return new Segment(text, colorArgb, shadow, bold, scale, hoverColorArgb, onClick);
        }

        Segment withBold(boolean bold) {
            return new Segment(text, colorArgb, shadow, bold, scale, hoverColorArgb, onClick);
        }

        Segment withScale(float scale) {
            return new Segment(text, colorArgb, shadow, bold, scale, hoverColorArgb, onClick);
        }

        Segment withHoverColor(Integer argb) {
            return new Segment(text, colorArgb, shadow, bold, scale, argb, onClick);
        }

        Segment withOnClick(Runnable callback) {
            return new Segment(text, colorArgb, shadow, bold, scale, hoverColorArgb, callback);
        }
    }

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
     * <p>Scale is applied via {@link AlumiteGuiRenderer#pushTransform} at draw time so the segment
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
     * Returns a copy that switches the root segment's color to the given ARGB value when the
     * pointer is within the text's drawn bounds.
     *
     * @param argb
     *         the ARGB color to use on hover
     *
     * @return a new {@code UiText} with the hover color set
     */
    public UiText hoverColor(int argb) {
        return mapRoot(root -> root.withHoverColor(argb));
    }

    /**
     * Returns a copy that switches the root segment's color to a theme-resolved value when the
     * pointer is within the text's drawn bounds.
     *
     * @param resolver
     *         a function that picks a color from a {@link UiTheme}
     *
     * @return a new {@code UiText} with the hover color set
     */
    public UiText hoverColor(Function<UiTheme, Integer> resolver) {
        return hoverColor(resolver.apply(UiThemeRepository.get()));
    }

    /**
     * Returns a copy that invokes {@code callback} when the user clicks within the text's drawn
     * bounds with the primary mouse button.
     *
     * @param callback
     *         action to invoke on primary click
     *
     * @return a new {@code UiText} with the click handler set
     */
    public UiText onClick(Runnable callback) {
        return mapRoot(root -> root.withOnClick(callback));
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
    public int width(AlumiteGuiRenderer frame) {
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
    public int height(AlumiteGuiRenderer frame) {
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
    public void draw(AlumiteGuiRenderer frame, int positionX, int positionY) {
        int lineHeight = height(frame);
        int cursorX = positionX;
        for (Segment segment : segments) {
            int segWidth = segment.width(frame);
            if (segment.isInteractive()) {
                segment.drawInteractive(frame, cursorX, positionY, segWidth, lineHeight);
            } else {
                segment.draw(frame, cursorX, positionY);
            }
            cursorX += segWidth;
        }
    }

    private static UiText single(String text) {
        List<Segment> list = new ArrayList<>(1);
        list.add(new Segment(text, DEFAULT_COLOR, false, false, 1.0f, null, null));
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

    private static boolean isPointerOver(AlumiteGuiRenderer frame, int x, int y, int w, int h) {
        float px = frame.pointerX();
        float py = frame.pointerY();
        return !Float.isNaN(px) && !Float.isNaN(py) && px >= x && px < x + w && py >= y && py < y + h;
    }
}
