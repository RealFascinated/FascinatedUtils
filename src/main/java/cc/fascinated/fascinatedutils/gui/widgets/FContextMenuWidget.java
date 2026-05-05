package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A generic floating context menu widget that positions itself within a given layout
 * area and renders a list of clickable items.
 *
 * <p>The widget fills its entire layout bounds (for pointer-blocking), but only draws
 * and responds to clicks within the computed menu rectangle. Clicking outside the menu
 * calls {@code onClose}.
 *
 * <p>Callers are responsible for calling {@code onClose} (or equivalent) inside each
 * {@link Item#onClick()} when the action should also dismiss the menu.
 */
public class FContextMenuWidget extends FWidget {

    /**
     * A single entry in the context menu.
     *
     * @param label
     *         supplies the display text (evaluated each render)
     * @param textColor
     *         ARGB override for the label text, or {@code null} to use the theme's primary text color
     * @param onClick
     *         invoked when the item is left-clicked
     */
    public record Item(Supplier<String> label, Integer textColor, Runnable onClick) {

        /**
         * Creates an item that uses the theme's primary text color.
         *
         * @param label
         *         supplies the display text
         * @param onClick
         *         invoked when the item is left-clicked
         */
        public Item(Supplier<String> label, Runnable onClick) {
            this(label, null, onClick);
        }
    }

    private static final float PAD_X = 2f;
    private static final float PAD_Y = 2f;
    private static final float ITEM_GAP = 1f;
    private static final float TEXT_LEFT_PAD = 6f;
    private static final float TEXT_RIGHT_PAD = 10f;
    private static final float MIN_ROW_WIDTH = 80f;

    private final Runnable onClose;
    private final List<Item> items;
    private final List<ItemRowWidget> rows;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;

    public FContextMenuWidget(float posX, float posY, Runnable onClose, List<Item> items) {
        this.onClose = onClose;
        this.items = List.copyOf(items);
        this.rows = new ArrayList<>(items.size());
        this.menuX = posX;
        this.menuY = posY;
        for (Item item : this.items) {
            ItemRowWidget row = new ItemRowWidget(item.onClick());
            rows.add(row);
            addChild(row);
        }
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public PointerHitKind pointerHitKind() {
        return PointerHitKind.BLOCK;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        for (ItemRowWidget row : rows) {
            if (row.mouseDown(pointerX, pointerY, button)) {
                return true;
            }
        }
        if (!isMenuHit(pointerX, pointerY)) {
            onClose.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        for (ItemRowWidget row : rows) {
            if (row.click(pointerX, pointerY, button)) {
                return true;
            }
        }
        if (!isMenuHit(pointerX, pointerY)) {
            onClose.run();
            return false;
        }
        return false;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);

        float rowHeight = Math.max(13f, measure.getFontHeight() + 6f);
        float rowWidth = MIN_ROW_WIDTH;
        for (Item item : items) {
            String label = item.label().get();
            if (label != null) {
                rowWidth = Math.max(rowWidth, measure.measureTextWidth(label, false) + TEXT_LEFT_PAD + TEXT_RIGHT_PAD);
            }
        }

        menuWidth = rowWidth + 2f * PAD_X;
        menuHeight = items.size() * rowHeight + Math.max(0, items.size() - 1) * ITEM_GAP + 2f * PAD_Y;

        menuX = Math.max(layoutX, Math.min(menuX, layoutX + layoutWidth - menuWidth));
        menuY = Math.max(layoutY, Math.min(menuY, layoutY + layoutHeight - menuHeight));

        float cursorY = menuY + PAD_Y;
        for (ItemRowWidget row : rows) {
            row.layout(measure, menuX + PAD_X, cursorY, rowWidth, rowHeight);
            cursorY += rowHeight + ITEM_GAP;
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float cornerRadius = Math.max(0.5f, Math.min(6f, Math.min(menuWidth, menuHeight) * 0.5f - 0.01f));
        graphics.fillRoundedRectFrame(menuX, menuY, menuWidth, menuHeight, cornerRadius,
                graphics.theme().border(), graphics.theme().contextMenuSurface(), 1f, 1f, RectCornerRoundMask.ALL);

        for (int index = 0; index < rows.size(); index++) {
            ItemRowWidget row = rows.get(index);
            if (frame.isHitTarget(row)) {
                float itemCornerRadius = Math.max(0.5f, Math.min(4f, Math.min(row.w(), row.h()) * 0.5f - 0.01f));
                graphics.fillRoundedRect(row.x(), row.y(), row.w(), row.h(), itemCornerRadius,
                        graphics.theme().moduleListRowHover(), RectCornerRoundMask.ALL);
            }
            String label = items.get(index).label().get();
            if (label == null) {
                label = "";
            }
            Integer colorOverride = items.get(index).textColor();
            int textColor = graphics.theme().textPrimary();
            if (colorOverride != null) {
                textColor = colorOverride.intValue();
            }
            float textY = row.y() + (row.h() - graphics.getFontCapHeight()) * 0.5f;
            graphics.drawMiniMessageText("<color:" + Colors.rgbHex(textColor) + ">" + label + "</color>",
                    row.x() + TEXT_LEFT_PAD, textY, false);
        }
    }

    private boolean isMenuHit(float pointerX, float pointerY) {
        return pointerX >= menuX && pointerX < menuX + menuWidth
                && pointerY >= menuY && pointerY < menuY + menuHeight;
    }

    private static class ItemRowWidget extends FWidget {
        private final Runnable onClick;

        ItemRowWidget(Runnable onClick) {
            this.onClick = onClick;
        }

        @Override
        public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
            setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        }

        @Override
        public PointerHitKind pointerHitKind() {
            return PointerHitKind.TARGET;
        }

        @Override
        public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
            return UiPointerCursor.HAND;
        }

        @Override
        public boolean click(float pointerX, float pointerY, int button) {
            if (button != 0 || !containsPoint(pointerX, pointerY)) {
                return false;
            }
            onClick.run();
            return true;
        }
    }
}
