package cc.fascinated.fascinatedutils.gui.toast;

import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlay;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlays;
import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;
import cc.fascinated.fascinatedutils.gui.node.display.AlphaNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Queues and animates {@link Toast}s stacked in the top-right corner. Each frame it composes a node tree of
 * {@link ToastNode}s that a {@link UiOverlay} lays out and draws; clicking a toast dismisses it.
 */
public class ToastManager {

    public static final ToastManager INSTANCE = new ToastManager();

    private static final float MARGIN_R = 12f;
    private static final float MARGIN_T = 12f;
    private static final float GAP = 6f;
    private static final float FADE_IN_SPD = 8f;
    private static final float FADE_OUT_SPD = 4f;
    private static final float SLIDE_SPD = 12f;
    private static final float Y_SPD = 10f;
    private static final float GONE_ALPHA = 0.02f;
    private static final int MAX_TOASTS = 5;
    private static final int MAX_PENDING = 16;

    private final ArrayDeque<Toast> pending = new ArrayDeque<>();
    private final List<Entry> entries = new ArrayList<>();
    private final UiOverlay overlay = UiOverlays.INSTANCE.register(new UiOverlay(this::composeToasts).guiFont(true).acceptsInput(true).recomposeEveryFrame(true), UiOverlays.ORDER_TOASTS);
    private Toast lastAdded;
    private float canvasWidth;
    private float frameDeltaSeconds;
    private int nextEntryId;

    private static boolean sameContent(Toast first, Toast second) {
        return first.type() == second.type() && first.title().equals(second.title()) && first.message().equals(second.message());
    }

    /**
     * Enqueues a toast for display. Safe to call from any thread. A toast identical to the previous one that is
     * still queued or on screen is dropped, and the queue keeps only the newest entries when it overflows.
     *
     * @param toast the toast to display
     */
    public synchronized void add(Toast toast) {
        if (lastAdded != null && sameContent(lastAdded, toast)) {
            return;
        }
        lastAdded = toast;
        pending.addLast(toast);
        while (pending.size() > MAX_PENDING) {
            pending.pollFirst();
        }
    }

    private synchronized Toast pollPending() {
        return pending.pollFirst();
    }

    private synchronized boolean hasPending() {
        return !pending.isEmpty();
    }

    private synchronized void forget(Toast toast) {
        if (lastAdded == toast) {
            lastAdded = null;
        }
    }

    /**
     * Ticks animations and draws all active toasts through the toast overlay. Hovering a toast pauses its countdown.
     *
     * @param graphics     the draw context to submit to
     * @param canvasWidth  logical UI canvas width
     * @param canvasHeight logical UI canvas height
     * @param pointerX     UI-space pointer X
     * @param pointerY     UI-space pointer Y
     * @param deltaSeconds elapsed seconds since last frame
     */
    public void render(GuiGraphicsExtractor graphics, float canvasWidth, float canvasHeight, float pointerX, float pointerY, float deltaSeconds) {
        if (entries.isEmpty() && !hasPending()) {
            return;
        }
        this.canvasWidth = canvasWidth;
        this.frameDeltaSeconds = deltaSeconds;
        overlay.render(graphics, canvasWidth, canvasHeight, pointerX, pointerY, deltaSeconds);
    }

    private UiNode composeToasts(UiStateStore stateStore) {
        tick(frameDeltaSeconds);
        PositionedNode<?> layer = new PositionedNode<>().full();
        for (Entry entry : entries) {
            ToastNode card = new ToastNode(entry.toast, entry::dismiss);
            entry.card = card;
            float alpha = entry.alpha.value();
            int renderX = Math.round(canvasWidth - ToastNode.TOAST_WIDTH - MARGIN_R + entry.slide.value());
            int renderY = Math.round(entry.y.value());
            layer.addChild(new AlphaNode(() -> alpha).columnGap(0).pos(renderX, renderY).setNodeId("toast-" + entry.id).addChild(card));
        }
        return layer;
    }

    private void tick(float delta) {
        while (entries.size() < MAX_TOASTS) {
            Toast next = pollPending();
            if (next == null) {
                break;
            }
            Entry entry = new Entry(nextEntryId++, next);
            // Start just above the top slot so older toasts and this one ease down together without overlapping
            entry.y.snap(MARGIN_T - entry.height() - GAP);
            entries.add(entry);
        }

        for (int idx = 0; idx < entries.size(); idx++) {
            Entry entry = entries.get(idx);
            if (!entry.hovered() || entry.dismissing) {
                entry.age += delta;
            }
            if (!entry.dismissing && entry.age >= entry.toast.durationSeconds()) {
                entry.dismiss();
            }
            entry.alpha.tick(delta);
            if (entry.dismissing && entry.alpha.value() <= GONE_ALPHA) {
                forget(entry.toast);
                entries.remove(idx);
                idx--;
                continue;
            }
            entry.slide.tick(delta);
        }

        // Newest at the top, older ones stack downward
        float stackTop = MARGIN_T;
        for (int idx = entries.size() - 1; idx >= 0; idx--) {
            Entry entry = entries.get(idx);
            entry.y.target(stackTop);
            entry.y.tick(delta);
            stackTop += entry.height() + GAP;
        }
    }

    private static class Entry {

        final int id;
        final Toast toast;
        final AnimHandle alpha = new AnimHandle(0f).target(1f).speed(FADE_IN_SPD);
        final AnimHandle slide = new AnimHandle(ToastNode.TOAST_WIDTH + MARGIN_R).target(0f).speed(SLIDE_SPD);
        final AnimHandle y = new AnimHandle(MARGIN_T).speed(Y_SPD);
        float age;
        boolean dismissing;
        // The card composed last frame; its laid-out height and hover state drive this frame's stacking and countdown
        ToastNode card;

        Entry(int id, Toast toast) {
            this.id = id;
            this.toast = toast;
        }

        boolean hovered() {
            return card != null && card.isHovered();
        }

        float height() {
            return card != null && card.bounds().height() > 0 ? card.bounds().height() : ToastNode.MIN_HEIGHT;
        }

        void dismiss() {
            if (!dismissing) {
                dismissing = true;
                alpha.target(0f).speed(FADE_OUT_SPD);
            }
        }
    }
}
