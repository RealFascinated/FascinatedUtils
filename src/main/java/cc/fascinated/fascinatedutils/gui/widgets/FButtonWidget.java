package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

public class FButtonWidget extends FWidget {
    private final int maxLabelLines;
    private final float layoutWidthLogical;
    private final float labelLineGapDesign;
    private final float verticalPadDesign;
    private final float heightScale;
    private final float horizontalTextPadDesign;
    private final float cornerRadiusDesign;
    protected boolean hovered;
    private Runnable onClick = () -> {
    };
    private Supplier<String> labelSupplier = () -> "";

    public FButtonWidget(Runnable onClick, Supplier<String> labelSupplier, float layoutWidthLogical) {
        this(onClick, labelSupplier, layoutWidthLogical, 2, 2f, 8f, 1f, 8f);
    }

    public FButtonWidget(Runnable onClick, Supplier<String> labelSupplier, float layoutWidthLogical, int maxLabelLines, float labelLineGapDesign, float verticalPadDesign, float heightScale, float horizontalTextPadDesign) {
        this(onClick, labelSupplier, layoutWidthLogical, maxLabelLines, labelLineGapDesign, verticalPadDesign, heightScale, horizontalTextPadDesign, -1f);
    }

    public FButtonWidget(Runnable onClick, Supplier<String> labelSupplier, float layoutWidthLogical, int maxLabelLines, float labelLineGapDesign, float verticalPadDesign, float heightScale, float horizontalTextPadDesign, float cornerRadiusDesign) {
        this.maxLabelLines = Math.max(1, maxLabelLines);
        this.layoutWidthLogical = Math.max(0f, layoutWidthLogical);
        this.labelLineGapDesign = labelLineGapDesign;
        this.verticalPadDesign = verticalPadDesign;
        this.heightScale = Math.max(0.1f, heightScale);
        this.horizontalTextPadDesign = horizontalTextPadDesign;
        this.cornerRadiusDesign = cornerRadiusDesign;
        setOnClick(onClick);
        setLabelSupplier(labelSupplier);
    }

    public static float wrappedLabelChipHeightPx(int maxLabelLines, float labelLineGapDesign, float verticalPadDesign, float heightScale) {
        return wrappedLabelChipHeightPxInner(maxLabelLines, labelLineGapDesign, verticalPadDesign, heightScale, modSettingsLabelLineHeightPxWithoutRenderer());
    }

    private static float modSettingsLabelLineHeightPxWithoutRenderer() {
        if (false) {
            return ModSettingsTheme.shellDesignBodyLineHeight();
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return ModSettingsTheme.shellDesignBodyLineHeight();
        }
        return Math.max(1f, client.font.lineHeight);
    }

    private static float labelLineHeightPx(UIRenderer measure) {
        return measure.getFontHeight();
    }

    private static float wrappedLabelChipHeightPxInner(int maxLabelLines, float labelLineGapDesign, float verticalPadDesign, float heightScale, float lineHeight) {
        float betweenLines = labelLineGapDesign;
        float labelBlock = maxLabelLines * lineHeight + Math.max(0, maxLabelLines - 1) * betweenLines;
        float fullHeight = labelBlock + verticalPadDesign;
        return fullHeight * heightScale;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick == null ? () -> {
        } : onClick;
    }

    public void setLabelSupplier(Supplier<String> labelSupplier) {
        this.labelSupplier = labelSupplier == null ? () -> "" : labelSupplier;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return layoutWidthLogical;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return wrappedLabelChipHeightPxInner(maxLabelLines, labelLineGapDesign, verticalPadDesign, heightScale, labelLineHeightPx(measure));
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
        if (button == 0) {
            onClick.run();
            return true;
        }
        return false;
    }

    protected int resolveButtonFillColorArgb(boolean hovered) {
        return FascinatedGuiTheme.INSTANCE.surface();
    }

    protected int resolveButtonBorderColorArgb(boolean hovered) {
        return hovered ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.border();
    }

    protected int resolveButtonLabelColorArgb(boolean hovered) {
        return FascinatedGuiTheme.INSTANCE.textPrimary();
    }

    protected float resolveCornerRadiusPx(GuiRenderer graphics) {
        float maxRadius = Math.min(w(), h()) * 0.5f - 0.01f;
        if (cornerRadiusDesign < 0f) {
            return Mth.clamp(graphics.theme().cardCornerRadius(), 0.5f, maxRadius - Math.min(UITheme.BORDER_THICKNESS_PX, UITheme.BORDER_THICKNESS_PX) * 0.5f);
        }
        return Math.max(0.5f, Math.min(cornerRadiusDesign, maxRadius));
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        int fillColor = resolveButtonFillColorArgb(hovered);
        int borderColor = resolveButtonBorderColorArgb(hovered);
        float borderThicknessX = UITheme.BORDER_THICKNESS_PX;
        float borderThicknessY = UITheme.BORDER_THICKNESS_PX;
        float cornerRadius = resolveCornerRadiusPx(graphics);
        graphics.fillRoundedRectFrame(x(), y(), w(), h(), cornerRadius, borderColor, fillColor, borderThicknessX, borderThicknessY, RectCornerRoundMask.ALL);
        String label = labelSupplier.get();
        if (label == null) {
            label = "";
        }
        float wrapBudget = w() - 2f * horizontalTextPadDesign;
        java.util.List<String> lines = TextLineLayout.wrapLines(label, wrapBudget, segment -> graphics.measureTextWidth(segment, false));
        float lineHeight = labelLineHeightPx(graphics);
        float betweenLines = labelLineGapDesign;
        float blockHeight = maxLabelLines * lineHeight + Math.max(0, maxLabelLines - 1) * betweenLines;
        float blockHeightClamped = Math.min(blockHeight, Math.max(lineHeight, h() - 4f));
        float startY = y() + Math.max(0f, h() - blockHeightClamped) * 0.5f;
        float cursorY = startY;
        int lineCount = Math.min(maxLabelLines, lines.size());
        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            String line = lines.get(lineIndex);
            int textWidth = graphics.measureTextWidth(line, false);
            float textX = x() + (w() - textWidth) * 0.5f;
            graphics.drawMiniMessageText("<color:" + Colors.rgbHex(resolveButtonLabelColorArgb(hovered)) + ">" + line + "</color>", textX, cursorY + (lineHeight - graphics.getFontCapHeight()) * 0.5f, false);
            cursorY += lineHeight + betweenLines;
        }
    }
}
