package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProfileContextMenuWidget extends FWidget {
    private final Runnable onClose;
    private final MenuItemWidget renameItem;
    private final MenuItemWidget deleteItem;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;

    public ProfileContextMenuWidget(float posX, float posY, Runnable onClose, Consumer<String> onAction) {
        this.onClose = onClose;

        renameItem = new MenuItemWidget(() -> {
            onAction.accept("rename");
            onClose.run();
        }, () -> Component.translatable("fascinatedutils.setting.shell.profile_context_rename").getString());
        deleteItem = new MenuItemWidget(() -> {
            onAction.accept("delete");
            onClose.run();
        }, () -> Component.translatable("fascinatedutils.setting.shell.profile_context_delete").getString());

        addChild(renameItem);
        addChild(deleteItem);

        menuX = posX;
        menuY = posY;
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
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (renameItem.mouseDown(pointerX, pointerY, button)) {
            return true;
        }
        if (deleteItem.mouseDown(pointerX, pointerY, button)) {
            return true;
        }
        if (!isPointerInside(pointerX, pointerY)) {
            onClose.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (renameItem.click(pointerX, pointerY, button)) {
            return true;
        }
        if (deleteItem.click(pointerX, pointerY, button)) {
            return true;
        }
        if (!isPointerInside(pointerX, pointerY)) {
            onClose.run();
            return true;
        }
        return false;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);

        float paddingX = 2f;
        float paddingY = 2f;
        float gap = 1f;
        float rowWidth = 112f;
        float rowHeight = Math.max(13f, measure.getFontHeight() + 6f);

        menuWidth = rowWidth + 2f * paddingX;
        menuHeight = 2f * rowHeight + gap + 2f * paddingY;

        float constrainedX = Math.max(layoutX, Math.min(menuX, layoutX + layoutWidth - menuWidth));
        float constrainedY = Math.max(layoutY, Math.min(menuY, layoutY + layoutHeight - menuHeight));

        menuX = constrainedX;
        menuY = constrainedY;

        float cursorY = menuY + paddingY;
        renameItem.layout(measure, menuX + paddingX, cursorY, rowWidth, rowHeight);
        cursorY += rowHeight + gap;
        deleteItem.layout(measure, menuX + paddingX, cursorY, rowWidth, rowHeight);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        float cornerRadius = Math.max(0.5f, Math.min(6f, Math.min(menuWidth, menuHeight) * 0.5f - 0.01f));
        float borderThickness = 1f;
        graphics.fillRoundedRectFrame(menuX, menuY, menuWidth, menuHeight, cornerRadius, graphics.theme().border(), graphics.theme().surface(), borderThickness, borderThickness, RectCornerRoundMask.ALL);
    }

    private boolean isPointerInside(float pointerX, float pointerY) {
        return pointerX >= menuX && pointerX < menuX + menuWidth && pointerY >= menuY && pointerY < menuY + menuHeight;
    }

    private static class MenuItemWidget extends FWidget {
        private final Runnable onClick;
        private final Supplier<String> labelSupplier;
        private boolean hovered;

        private MenuItemWidget(Runnable onClick, Supplier<String> labelSupplier) {
            this.onClick = onClick;
            this.labelSupplier = labelSupplier;
        }

        @Override
        public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
            setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        }

        @Override
        public boolean wantsPointer() {
            return true;
        }

        @Override
        public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
            return UiPointerCursor.HAND;
        }

        @Override
        public boolean mouseEnter(float pointerX, float pointerY) {
            hovered = true;
            return false;
        }

        @Override
        public boolean mouseLeave(float pointerX, float pointerY) {
            hovered = false;
            return false;
        }

        @Override
        public boolean click(float pointerX, float pointerY, int button) {
            if (button != 0) {
                return false;
            }
            onClick.run();
            return true;
        }

        @Override
        protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
            if (hovered) {
                float cornerRadius = Math.max(0.5f, Math.min(4f, Math.min(w(), h()) * 0.5f - 0.01f));
                graphics.fillRoundedRect(x(), y(), w(), h(), cornerRadius, graphics.theme().moduleListRowHover(), RectCornerRoundMask.ALL);
            }

            String label = labelSupplier.get();
            if (label == null) {
                label = "";
            }
            float leftPad = 6f;
            float textY = y() + (h() - graphics.getFontHeight()) * 0.5f;
            graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(graphics.theme().textPrimary()) + ">" + label + "</color>", x() + leftPad, textY, false);
        }
    }
}
