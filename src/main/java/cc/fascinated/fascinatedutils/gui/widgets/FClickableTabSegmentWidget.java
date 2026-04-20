package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

public class FClickableTabSegmentWidget extends FWidget {
    private final String tabKey;
    private final String labelText;
    private final Callback<String> onSelect;
    private String selectedKey;
    private boolean hovered;
    private float shellSegmentCornerRadius;
    private int shellSegmentCornerMask = RectCornerRoundMask.ALL;

    public FClickableTabSegmentWidget(String tabKey, String labelText, Callback<String> onSelect) {
        this.tabKey = tabKey;
        this.labelText = labelText;
        this.onSelect = onSelect;
    }

    public void setSelectedKey(String selectedKey) {
        this.selectedKey = selectedKey;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void setShellSegmentFillet(float cornerRadius, int cornerRoundMask) {
        this.shellSegmentCornerRadius = cornerRadius;
        this.shellSegmentCornerMask = cornerRoundMask;
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
        if (button == 0) {
            onSelect.invoke(tabKey);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean selected = tabKey.equals(selectedKey);
        int fillColor = selected ? graphics.theme().moduleListRowSelected() : hovered ? graphics.theme().moduleListRowHover() : graphics.theme().surface();
        int textColor = selected ? graphics.theme().textAccent() : hovered ? graphics.theme().textPrimary() : graphics.theme().textMuted();
        if (shellSegmentCornerRadius > 0.5f) {
            float borderThickness = 1f;
            int borderArgb = hovered ? graphics.theme().borderHover() : graphics.theme().border();
            graphics.fillRoundedRectFrame(x(), y(), w(), h(), shellSegmentCornerRadius, borderArgb, fillColor, borderThickness, borderThickness, shellSegmentCornerMask);
        }
        else {
            graphics.drawRect(x(), y(), w(), h(), fillColor);
            float borderThickness = 1f;
            int borderArgb = hovered ? graphics.theme().borderHover() : graphics.theme().border();
            graphics.drawBorder(x(), y(), w(), h(), borderThickness, borderArgb);
        }
        int textWidth = graphics.measureTextWidth(labelText, false);
        float textX = x() + (w() - textWidth) * 0.5f;
        float lineHeight = graphics.getFontHeight();
        float textY = y() + (h() - lineHeight) * 0.5f;
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(textColor) + ">" + labelText + "</color>", textX, textY, false);
    }
}
