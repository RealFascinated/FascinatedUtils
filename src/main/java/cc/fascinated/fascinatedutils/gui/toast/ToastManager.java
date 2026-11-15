package cc.fascinated.fascinatedutils.gui.toast;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ToastManager {

    public static final ToastManager INSTANCE = new ToastManager();

    private static final float TOAST_W = 220f;
    private static final float TOAST_H = 46f;
    private static final float MARGIN_R = 12f;
    private static final float MARGIN_T = 12f;
    private static final float GAP = 6f;
    private static final float CORNER_R = 6f;
    private static final float ACCENT_W = 4f;
    private static final float ICON_SLOT_W = 28f;
    private static final float TITLE_GAP = 4f;
    private static final float PROGRESS_H = 3f;
    private static final float FADE_IN_SPD = 8f;
    private static final float FADE_OUT_SPD = 4f;
    private static final float SLIDE_SPD = 12f;
    private static final float Y_LERP_SPD = 10f;
    private static final int MAX_TOASTS = 5;
    private static final float LINE_GAP = 2f;
    private static final int MAX_LINES = 4;

    private static final int SUCCESS_ACCENT_COLOR = 0xFF44CC77;
    private static final int ERROR_ACCENT_COLOR = 0xFFFF4444;
    private static final int WARNING_ACCENT_COLOR = 0xFFFFAA33;
    private static final int INFO_ACCENT_COLOR = 0xFF5599EE;
    private static final int CARD_FILL_COLOR = 0xF01A1E24;
    private static final int CARD_FILL_HOVERED_COLOR = 0xF5222833;
    private static final int CARD_BORDER_COLOR = 0x30FFFFFF;
    private static final int MESSAGE_TEXT_COLOR = 0xFFCCCCCC;
    private static final int PROGRESS_TRACK_COLOR = 0x20FFFFFF;

    private final ConcurrentLinkedQueue<Toast> pending = new ConcurrentLinkedQueue<>();
    private final List<Entry> entries = new ArrayList<>();

    private static int accentColor(Toast.Type type) {
        return switch (type) {
            case SUCCESS -> SUCCESS_ACCENT_COLOR;
            case ERROR -> ERROR_ACCENT_COLOR;
            case WARNING -> WARNING_ACCENT_COLOR;
            case INFO -> INFO_ACCENT_COLOR;
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
            }
            else {
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
        return lines;
    }

    private static Identifier iconSprite(Toast.Type type) {
        return switch (type) {
            case SUCCESS -> ModUiTextures.CHECK.getId();
            case ERROR -> ModUiTextures.CLOSE.getId();
            case WARNING -> ModUiTextures.WARNING.getId();
            case INFO -> null;
        };
    }

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
    public void render(GuiRenderer renderer, float uiWidth, float uiHeight, float mouseX, float mouseY, float deltaSeconds) {
        tick(deltaSeconds);
        for (Entry entry : entries) {
            float renderX = uiWidth - TOAST_W - MARGIN_R + entry.slideOffset;
            renderEntry(renderer, entry, renderX, mouseX, mouseY);
        }
    }

    private void tick(float delta) {
        while (entries.size() < MAX_TOASTS) {
            Toast next = pending.poll();
            if (next == null) {
                break;
            }
            Entry entry = new Entry(next);
            // Snap renderY to the top slot; slide-in handles the horizontal entry
            entry.renderY = MARGIN_T;
            entries.add(entry);
        }

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
            }
            else {
                entry.alpha = Math.min(1f, entry.alpha + FADE_IN_SPD * delta);
            }

            entry.slideOffset = Math.max(0f, entry.slideOffset * (1f - SLIDE_SPD * delta));
            if (entry.slideOffset < 0.5f) {
                entry.slideOffset = 0f;
            }
        }

        // Lerp each entry toward its stacking target (newest at top, older ones stack downward)
        int count = entries.size();
        float stackTop = MARGIN_T;
        for (int idx = count - 1; idx >= 0; idx--) {
            Entry entry = entries.get(idx);
            float targetY = stackTop;
            entry.renderY += (targetY - entry.renderY) * Math.min(1f, Y_LERP_SPD * delta);
            stackTop += entry.effectiveHeight + GAP;
        }
    }

    private void renderEntry(GuiRenderer renderer, Entry entry, float renderX, float mouseX, float mouseY) {
        float renderY = entry.renderY;

        // Text area geometry (after accent stripe + icon slot)
        float textStartX = renderX + ACCENT_W + ICON_SLOT_W;
        float textClipW = TOAST_W - ACCENT_W - ICON_SLOT_W - 8f;

        if (entry.wrappedLines == null) {
            entry.wrappedLines = wrapText(renderer, entry.toast.message(), textClipW);
            float capH = renderer.fontCapHeight();
            float lineH = renderer.fontHeight();
            int lineCount = entry.wrappedLines.size();
            float msgH = lineCount > 0 ? lineCount * lineH + (lineCount - 1) * LINE_GAP + TITLE_GAP : 0f;
            float padV = (TOAST_H - PROGRESS_H - capH - (lineCount > 0 ? TITLE_GAP + lineH : 0f)) / 2f;
            entry.effectiveHeight = Math.max(TOAST_H, padV + capH + msgH + padV + PROGRESS_H);
        }
        float effectiveH = entry.effectiveHeight;

        boolean hovered = mouseX >= renderX && mouseX < renderX + TOAST_W && mouseY >= renderY && mouseY < renderY + effectiveH;
        int accentColor = accentColor(entry.toast.type());

        renderer.setMultiplyAlpha(entry.alpha);

        int cardFill = hovered ? CARD_FILL_HOVERED_COLOR : CARD_FILL_COLOR;
        renderer.fillRoundedRectFrame(renderX, renderY, TOAST_W, effectiveH, CORNER_R, CARD_BORDER_COLOR, cardFill, 1f, 1f, RectCornerRoundMask.ALL);

        // Left accent stripe — left corners match card radius, right corners square
        renderer.fillRoundedRect(renderX, renderY, ACCENT_W, effectiveH, CORNER_R, accentColor, RectCornerRoundMask.LEFT);

        // Vertical layout: pad top, then title row, then TITLE_GAP, then message rows
        float capH = renderer.fontCapHeight();
        float lineH = renderer.fontHeight();
        float padV = (TOAST_H - PROGRESS_H - capH - TITLE_GAP - lineH) / 2f;
        float titleY = renderY + padV;
        float msgY = titleY + capH + TITLE_GAP;

        // Icon — vertically centered in the full usable height (above progress bar)
        float iconCenterX = renderX + ACCENT_W + ICON_SLOT_W / 2f;
        float iconY = renderY + (effectiveH - PROGRESS_H - capH) / 2f;
        Identifier iconSprite = iconSprite(entry.toast.type());
        if (iconSprite != null) {
            renderer.drawSprite(iconSprite, iconCenterX - capH / 2f, iconY, capH, capH, accentColor);
        } else {
            renderer.drawCenteredText("ℹ", iconCenterX, iconY, accentColor, false, true);
        }

        renderer.pushClip(textStartX, renderY, textClipW, effectiveH);
        renderer.drawText(entry.toast.title(), textStartX, titleY, accentColor, false, true);

        for (int lineIdx = 0; lineIdx < entry.wrappedLines.size(); lineIdx++) {
            float lineY = msgY + lineIdx * (lineH + LINE_GAP);
            renderer.drawText(entry.wrappedLines.get(lineIdx), textStartX, lineY, MESSAGE_TEXT_COLOR, false, false);
        }
        renderer.popClip();

        // Progress bar — track across the bottom, fill depletes from the right
        float barX = renderX + ACCENT_W;
        float barY = renderY + effectiveH - PROGRESS_H;
        float barContentW = TOAST_W - ACCENT_W;
        float progress = Math.max(0f, 1f - entry.age / entry.toast.durationSeconds());
        renderer.drawRect(barX, barY, barContentW, PROGRESS_H, PROGRESS_TRACK_COLOR);
        if (progress > 0f) {
            float filledW = progress * barContentW;
            renderer.drawRect(barX, barY, filledW, PROGRESS_H, (accentColor & 0x00FFFFFF) | 0xBB000000);
        }

        renderer.resetMultiplyAlpha();
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
            this.toast = toast;
            this.slideOffset = TOAST_W + MARGIN_R; // starts fully off-screen right
        }
    }
}
