package cc.fascinated.fascinatedutils.gui.toast;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ToastManager {

    public static final ToastManager INSTANCE = new ToastManager();

    private static final float TOAST_W      = 220f;
    private static final float TOAST_H      = 44f;
    private static final float MARGIN_R     = 12f;
    private static final float MARGIN_B     = 12f;
    private static final float GAP          = 6f;
    private static final float CORNER_R     = UITheme.CORNER_RADIUS_MD;
    private static final float ACCENT_W     = 4f;
    private static final float PROGRESS_H   = 3f;
    private static final float FADE_IN_SPD  = 8f;
    private static final float FADE_OUT_SPD = 4f;
    private static final float SLIDE_SPD    = 12f;
    private static final float Y_LERP_SPD   = 10f;
    private static final int   MAX_TOASTS   = 5;
    private static final float LINE_GAP     = 2f;
    private static final int   MAX_LINES    = 4;

    private final ConcurrentLinkedQueue<Toast> pending = new ConcurrentLinkedQueue<>();
    private final List<Entry> entries = new ArrayList<>();
    private float lastUiWidth;

    /**
     * Enqueues a toast for display. Safe to call from any thread.
     *
     * @param toast the toast to display
     */
    public void add(Toast toast) {
        pending.add(toast);
    }

    /**
     * Ticks animations and renders all active toasts.
     *
     * <p>Call this between {@code renderer.begin()} and {@code renderer.end()} in any
     * screen's {@code renderCustom} method.
     *
     * @param renderer     the active {@link GuiRenderer}
     * @param uiWidth      logical UI canvas width
     * @param uiHeight     logical UI canvas height
     * @param mouseX       UI-space pointer X
     * @param mouseY       UI-space pointer Y
     * @param deltaSeconds elapsed seconds since last frame
     */
    public void render(GuiRenderer renderer, float uiWidth, float uiHeight,
                       float mouseX, float mouseY, float deltaSeconds) {
        lastUiWidth = uiWidth;
        tick(deltaSeconds, uiHeight);
        for (Entry entry : entries) {
            float renderX = uiWidth - TOAST_W - MARGIN_R + entry.slideOffset;
            renderEntry(renderer, entry, renderX, mouseX, mouseY);
        }
    }

    /**
     * Dismisses the topmost toast under the pointer, if any.
     *
     * @param pointerX UI-space pointer X
     * @param pointerY UI-space pointer Y
     * @return {@code true} if a toast consumed the click
     */
    public boolean mouseDown(float pointerX, float pointerY) {
        float baseX = lastUiWidth - TOAST_W - MARGIN_R;
        for (int idx = entries.size() - 1; idx >= 0; idx--) {
            Entry entry = entries.get(idx);
            float renderX = baseX + entry.slideOffset;
            if (pointerX >= renderX && pointerX < renderX + TOAST_W
                    && pointerY >= entry.renderY && pointerY < entry.renderY + entry.effectiveHeight) {
                entry.dismissing = true;
                return true;
            }
        }
        return false;
    }

    private void tick(float delta, float uiHeight) {
        // Drain pending queue onto the main-thread list
        Toast next;
        while ((next = pending.poll()) != null) {
            if (entries.size() < MAX_TOASTS) {
                Entry entry = new Entry(next);
                // Snap renderY to the bottom slot; slide-in handles the horizontal entry
                entry.renderY = uiHeight - MARGIN_B - TOAST_H;
                entries.add(entry);
            }
        }

        // Age, fade, and slide each entry
        for (int idx = 0; idx < entries.size(); idx++) {
            Entry entry = entries.get(idx);
            entry.age += delta;

            if (!entry.dismissing && entry.age >= entry.toast.durationSeconds()) {
                entry.dismissing = true;
            }

            if (entry.dismissing) {
                entry.alpha = Math.max(0f, entry.alpha - FADE_OUT_SPD * delta);
                if (entry.alpha <= 0f) {
                    entries.remove(idx);
                    idx--;
                    continue;
                }
            } else {
                entry.alpha = Math.min(1f, entry.alpha + FADE_IN_SPD * delta);
            }

            entry.slideOffset = Math.max(0f, entry.slideOffset * (1f - SLIDE_SPD * delta));
            if (entry.slideOffset < 0.5f) {
                entry.slideOffset = 0f;
            }
        }

        // Lerp each entry toward its stacking target (newest at bottom, oldest above)
        int count = entries.size();
        float stackBottom = uiHeight - MARGIN_B;
        for (int idx = count - 1; idx >= 0; idx--) {
            Entry entry = entries.get(idx);
            float targetY = stackBottom - entry.effectiveHeight;
            entry.renderY += (targetY - entry.renderY) * Math.min(1f, Y_LERP_SPD * delta);
            stackBottom -= entry.effectiveHeight + GAP;
        }
    }

    private void renderEntry(GuiRenderer renderer, Entry entry,
                             float renderX, float mouseX, float mouseY) {
        float renderY    = entry.renderY;

        // Message area geometry (used for wrapping and clipping)
        float msgX     = renderX + ACCENT_W + 22f;
        float msgClipW = TOAST_W - ACCENT_W - 22f - 8f;

        // Compute wrapped lines and effective height on first render
        if (entry.wrappedLines == null) {
            entry.wrappedLines = wrapText(renderer, entry.toast.message(), msgClipW);
            if (entry.wrappedLines.size() > 1) {
                float padV     = (TOAST_H - PROGRESS_H - renderer.getFontCapHeight()) / 2f;
                float lineH    = renderer.getFontHeight();
                int   lineCount = entry.wrappedLines.size();
                float contentH = lineCount * lineH + (lineCount - 1) * LINE_GAP;
                entry.effectiveHeight = Math.max(TOAST_H, padV * 2f + contentH + PROGRESS_H);
            }
        }
        float effectiveH = entry.effectiveHeight;

        boolean hovered  = mouseX >= renderX && mouseX < renderX + TOAST_W
                        && mouseY >= renderY && mouseY < renderY + effectiveH;
        int accentColor  = accentColor(entry.toast.type());

        renderer.setMultiplyAlpha(entry.alpha);

        // Card background + border
        int cardFill = hovered ? 0xF5222833 : 0xF01A1E24;
        renderer.fillRoundedRectFrame(renderX, renderY, TOAST_W, effectiveH, CORNER_R,
                0x25FFFFFF, cardFill, 1f, 1f, RectCornerRoundMask.ALL);

        // Left accent stripe — left corners match card radius, right corners square
        renderer.fillRoundedRect(renderX, renderY, ACCENT_W, effectiveH, CORNER_R,
                accentColor, RectCornerRoundMask.LEFT);

        // Vertically center content above the progress bar
        float padV     = (TOAST_H - PROGRESS_H - renderer.getFontCapHeight()) / 2f;
        float lineH    = renderer.getFontHeight();
        float firstLineY = renderY + padV;

        // Icon (bold, accent-colored, centered in the icon slot at first-line height)
        renderer.drawCenteredText(icon(entry.toast.type()),
                renderX + ACCENT_W + 12f, firstLineY, accentColor, false, true);

        // Message text — draw each wrapped line
        renderer.pushClip(msgX, renderY, msgClipW, effectiveH);
        for (int lineIdx = 0; lineIdx < entry.wrappedLines.size(); lineIdx++) {
            float lineY = firstLineY + lineIdx * (lineH + LINE_GAP);
            renderer.drawText(entry.wrappedLines.get(lineIdx), msgX, lineY, 0xFFFFFFFF, false, false);
        }
        renderer.popClip();

        // Progress bar track + fill along the bottom edge
        float barX         = renderX + ACCENT_W;
        float barY         = renderY + effectiveH - PROGRESS_H;
        float barContentW  = TOAST_W - ACCENT_W;
        float progress     = Math.max(0f, 1f - entry.age / entry.toast.durationSeconds());
        renderer.drawRect(barX, barY, barContentW, PROGRESS_H, 0x20FFFFFF);
        if (progress > 0f) {
            renderer.drawRect(barX, barY, progress * barContentW, PROGRESS_H,
                    (accentColor & 0x00FFFFFF) | 0xBB000000);
        }

        renderer.resetMultiplyAlpha();
    }

    private static int accentColor(Toast.Type type) {
        return switch (type) {
            case SUCCESS -> 0xFF44CC77;
            case ERROR   -> 0xFFFF4444;
            case WARNING -> 0xFFFFAA33;
            case INFO    -> 0xFF5599EE;
        };
    }

    private static List<String> wrapText(GuiRenderer renderer, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ", -1);
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (renderer.measureTextWidth(candidate, false) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                // If a single word exceeds max width, add it anyway to avoid infinite loop
                current = new StringBuilder(word);
            }
            if (lines.size() == MAX_LINES - 1) {
                break;
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add(text);
        }
        return lines;
    }

    private static String icon(Toast.Type type) {
        return switch (type) {
            case SUCCESS -> "\u2713"; // ✓
            case ERROR   -> "\u2715"; // ✕
            case WARNING -> "\u26A0"; // ⚠
            case INFO    -> "\u2139"; // ℹ
        };
    }

    private static class Entry {

        final Toast toast;
        float age;
        float alpha;
        float slideOffset;
        float renderY;
        boolean dismissing;
        List<String> wrappedLines;
        float effectiveHeight = TOAST_H;

        Entry(Toast toast) {
            this.toast       = toast;
            this.slideOffset = TOAST_W + MARGIN_R; // starts fully off-screen right
        }
    }
}
