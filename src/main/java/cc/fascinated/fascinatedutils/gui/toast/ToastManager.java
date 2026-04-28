package cc.fascinated.fascinatedutils.gui.toast;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;

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
    private static final float CORNER_R     = 6f;
    private static final float ACCENT_W     = 4f;
    private static final float PROGRESS_H   = 3f;
    private static final float FADE_IN_SPD  = 8f;
    private static final float FADE_OUT_SPD = 4f;
    private static final float SLIDE_SPD    = 12f;
    private static final float Y_LERP_SPD   = 10f;
    private static final int   MAX_TOASTS   = 5;

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
                    && pointerY >= entry.renderY && pointerY < entry.renderY + TOAST_H) {
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
        for (int idx = 0; idx < count; idx++) {
            Entry entry = entries.get(idx);
            float targetY = uiHeight - MARGIN_B - TOAST_H - (count - 1 - idx) * (TOAST_H + GAP);
            entry.renderY += (targetY - entry.renderY) * Math.min(1f, Y_LERP_SPD * delta);
        }
    }

    private void renderEntry(GuiRenderer renderer, Entry entry,
                             float renderX, float mouseX, float mouseY) {
        float renderY    = entry.renderY;
        boolean hovered  = mouseX >= renderX && mouseX < renderX + TOAST_W
                        && mouseY >= renderY && mouseY < renderY + TOAST_H;
        int accentColor  = accentColor(entry.toast.type());

        renderer.setMultiplyAlpha(entry.alpha);

        // Card background + border
        int cardFill = hovered ? 0xF5222833 : 0xF01A1E24;
        renderer.fillRoundedRectFrame(renderX, renderY, TOAST_W, TOAST_H, CORNER_R,
                0x25FFFFFF, cardFill, 1f, 1f, RectCornerRoundMask.ALL);

        // Left accent stripe — left corners match card radius, right corners square
        renderer.fillRoundedRect(renderX, renderY, ACCENT_W, TOAST_H, CORNER_R,
                accentColor, RectCornerRoundMask.LEFT);

        // Vertically center icon + message above the progress bar
        float contentY = renderY + (TOAST_H - PROGRESS_H - renderer.getFontCapHeight()) / 2f;

        // Icon (bold, accent-colored, centered in the icon slot)
        renderer.drawCenteredText(icon(entry.toast.type()),
                renderX + ACCENT_W + 12f, contentY, accentColor, false, true);

        // Message text, clipped to the content area
        float msgX      = renderX + ACCENT_W + 22f;
        float msgClipW  = TOAST_W - ACCENT_W - 22f - 8f;
        renderer.pushClip(msgX, renderY, msgClipW, TOAST_H);
        renderer.drawText(entry.toast.message(), msgX, contentY, 0xFFFFFFFF, false, false);
        renderer.popClip();

        // Progress bar track + fill along the bottom edge
        float barX         = renderX + ACCENT_W;
        float barY         = renderY + TOAST_H - PROGRESS_H;
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

        Entry(Toast toast) {
            this.toast       = toast;
            this.slideOffset = TOAST_W + MARGIN_R; // starts fully off-screen right
        }
    }
}
