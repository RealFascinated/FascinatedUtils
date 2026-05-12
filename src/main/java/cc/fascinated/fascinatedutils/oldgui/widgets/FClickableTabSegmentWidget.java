package cc.fascinated.fascinatedutils.oldgui.widgets;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.oldgui.core.Callback;
import cc.fascinated.fascinatedutils.oldgui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.oldgui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import lombok.Setter;

public class FClickableTabSegmentWidget extends FWidget {
    private final String tabKey;
    private final String labelText;
    private final Callback<String> onSelect;
    @Setter
    private String selectedKey;
    private float shellSegmentCornerRadius;
    private int shellSegmentCornerMask = RectCornerRoundMask.ALL;

    public FClickableTabSegmentWidget(String tabKey, String labelText, Callback<String> onSelect) {
        this.tabKey = tabKey;
        this.labelText = labelText;
        this.onSelect = onSelect;
    }

    public void setShellSegmentFillet(float cornerRadius, int cornerRoundMask) {
        this.shellSegmentCornerRadius = cornerRadius;
        this.shellSegmentCornerMask = cornerRoundMask;
    }

    public String getLabelText() {
        return labelText;
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
    public boolean click(float pointerX, float pointerY, int button) {
        if (button == 0) {
            onSelect.invoke(tabKey);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        boolean hovered = frame.isHitTarget(this);
        boolean selected = tabKey.equals(selectedKey);
        int fillColor = selected ? graphics.theme().accent() : hovered ? graphics.theme().moduleListRowHover() : 0x20ffffff;
        int textColor = selected ? graphics.theme().textPrimary() : graphics.theme().textMuted();
        if (shellSegmentCornerRadius > 0.5f) {
            float borderThickness = 1f;
            int borderArgb = selected ? graphics.theme().accent() : hovered ? graphics.theme().borderHover() : 0x33ffffff;
            graphics.fillRoundedRectFrame(x(), y(), w(), h(), shellSegmentCornerRadius, borderArgb, fillColor, borderThickness, borderThickness, shellSegmentCornerMask);
        }
        else {
            graphics.drawRect(x(), y(), w(), h(), fillColor);
        }
        int textWidth = graphics.measureTextWidth(labelText, false);
        float textX = x() + (w() - textWidth) * 0.5f;
        float lineHeight = graphics.getFontCapHeight();
        float textY = y() + (h() - lineHeight) * 0.5f;
        graphics.drawMiniMessageText("<color:" + Colors.rgbHex(textColor) + ">" + labelText + "</color>", textX, textY, false);
    }
}
