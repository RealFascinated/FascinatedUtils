package cc.fascinated.fascinatedutils.gui.toast;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;
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
    private static final float ICON_SLOT_W = 30f;
    private static final float TITLE_GAP = 4f;
    private static final float FADE_IN_SPD = 8f;
    private static final float FADE_OUT_SPD = 4f;
    private static final float SLIDE_SPD = 12f;
    private static final float Y_LERP_SPD = 10f;
    private static final int MAX_TOASTS = 5;
    private static final float LINE_GAP = 2f;
    private static final int MAX_LINES = 4;

    private final ConcurrentLinkedQueue<Toast> pending = new ConcurrentLinkedQueue<>();
    private final List<Entry> entries = new ArrayList<>();

    private static int accentColor(UiTheme theme, Toast.Type type) {
        return switch (type) {
            case SUCCESS -> theme.success();
            case ERROR -> theme.danger();
            case WARNING -> theme.warning();
            case INFO -> theme.accent();
        };
    }

    private static List<String> wrapText(AlumiteGuiRenderer renderer, String text, float maxWidth) {
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
     * @param renderer     the active {@link AlumiteGuiRenderer}
     * @param uiWidth      logical UI canvas width
     * @param uiHeight     logical UI canvas height
     * @param mouseX       UI-space pointer X
     * @param mouseY       UI-space pointer Y
     * @param deltaSeconds elapsed seconds since last frame
     */
    public void render(AlumiteGuiRenderer renderer, float uiWidth, float uiHeight, float mouseX, float mouseY, float deltaSeconds) {
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

    private void renderEntry(AlumiteGuiRenderer renderer, Entry entry, float renderX, float mouseX, float mouseY) {
        float renderY = entry.renderY;

        // Text area geometry (after accent stripe + icon slot)
        float textStartX = renderX + ICON_SLOT_W;
        float textClipW = TOAST_W - ICON_SLOT_W - 10f;

        if (entry.wrappedLines == null) {
            entry.wrappedLines = wrapText(renderer, entry.toast.message(), textClipW);
            float capH = renderer.fontCapHeight();
            float lineH = renderer.fontHeight();
            int lineCount = entry.wrappedLines.size();
            float msgH = lineCount > 0 ? lineCount * lineH + (lineCount - 1) * LINE_GAP + TITLE_GAP : 0f;
            float padV = (TOAST_H - capH - (lineCount > 0 ? TITLE_GAP + lineH : 0f)) / 2f;
            entry.effectiveHeight = Math.max(TOAST_H, padV + capH + msgH + padV);
        }
        float effectiveH = entry.effectiveHeight;

        boolean hovered = mouseX >= renderX && mouseX < renderX + TOAST_W && mouseY >= renderY && mouseY < renderY + effectiveH;
        UiTheme theme = UiThemeRepository.get();
        int accentColor = accentColor(theme, entry.toast.type());

        renderer.setMultiplyAlpha(entry.alpha);

        int cardFill = hovered ? theme.surfaceActive() : theme.surfaceRaised();
        renderer.fillRoundedRectFrame(renderX, renderY, TOAST_W, effectiveH, CORNER_R, theme.borderSubtle(), cardFill, 1f, 1f, RectCornerRoundMask.ALL);

        // Vertical layout: pad top, then title row, then TITLE_GAP, then message rows
        float capH = renderer.fontCapHeight();
        float lineH = renderer.fontHeight();
        float padV = (TOAST_H - capH - TITLE_GAP - lineH) / 2f;
        float titleY = renderY + padV;
        float msgY = titleY + capH + TITLE_GAP;

        // Icon — vertically centered
        float iconCenterX = renderX + ICON_SLOT_W / 2f;
        float iconY = renderY + (effectiveH - capH) / 2f;
        Identifier iconSprite = iconSprite(entry.toast.type());
        if (iconSprite != null) {
            renderer.drawSprite(iconSprite, iconCenterX - capH / 2f, iconY, capH, capH, accentColor);
        } else {
            renderer.drawCenteredText("ℹ", iconCenterX, iconY, accentColor, false, true);
        }

        renderer.pushClip(textStartX, renderY, textClipW, effectiveH);
        renderer.drawText(entry.toast.title(), textStartX, titleY, theme.textPrimary(), false, true);

        for (int lineIdx = 0; lineIdx < entry.wrappedLines.size(); lineIdx++) {
            float lineY = msgY + lineIdx * (lineH + LINE_GAP);
            renderer.drawText(entry.wrappedLines.get(lineIdx), textStartX, lineY, theme.textMuted(), false, false);
        }
        renderer.popClip();

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
