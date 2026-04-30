package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.GuiTheme;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FIconCheckboxWidget extends FWidget {
    private static final float CHECKBOX_SIZE_DESIGN = 12f;
    private static final float CHECKBOX_CORNER_RADIUS_DESIGN = UITheme.CORNER_RADIUS_XS;
    private static final float LABEL_GAP_DESIGN = 4f;
    private final Consumer<Boolean> onToggle;
    private Supplier<String> labelSupplier;
    private Identifier checkedTextureId = ModUiTextures.CHECK.getId();
    private float outerWidth;
    @Setter
    @Getter
    private boolean checked;
    @Setter
    @Getter
    private boolean disabled;

    public FIconCheckboxWidget(boolean checked, Consumer<Boolean> onToggle, Supplier<String> labelSupplier, float outerWidth) {
        this.checked = checked;
        this.onToggle = Objects.requireNonNull(onToggle, "onToggle is required");
        this.labelSupplier = Objects.requireNonNull(labelSupplier, "labelSupplier is required");
        this.outerWidth = Math.max(0f, outerWidth);
    }

    private static int dimColor(int argb, float factor) {
        float clampedFactor = Math.max(0f, Math.min(1f, factor));
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int dimmedAlpha = Math.round(alpha * clampedFactor);
        int dimmedRed = Math.round(red * clampedFactor);
        int dimmedGreen = Math.round(green * clampedFactor);
        int dimmedBlue = Math.round(blue * clampedFactor);
        return (dimmedAlpha << 24) | (dimmedRed << 16) | (dimmedGreen << 8) | dimmedBlue;
    }

    public void setCheckedTextureId(Identifier checkedTextureId) {
        this.checkedTextureId = Objects.requireNonNull(checkedTextureId, "checkedTextureId is required");
    }

    public void setLabelSupplier(Supplier<String> labelSupplier) {
        this.labelSupplier = Objects.requireNonNull(labelSupplier, "labelSupplier is required");
    }

    public void setOuterWidth(float outerWidth) {
        this.outerWidth = Math.max(0f, outerWidth);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        float boxSize = CHECKBOX_SIZE_DESIGN;
        return Math.max(boxSize, measure.getFontHeight());
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return outerWidth;
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
        if (disabled) {
            return UiPointerCursor.NOT_ALLOWED;
        }
        return UiPointerCursor.HAND;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        return button == 0;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (disabled) {
            return true;
        }
        checked = !checked;
        onToggle.accept(checked);
        return true;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        float boxSize = CHECKBOX_SIZE_DESIGN;
        float boxCornerRadius = CHECKBOX_CORNER_RADIUS_DESIGN;
        float borderPx = 1f;
        float labelGap = LABEL_GAP_DESIGN;
        float boxX = x();
        float boxY = y() + (h() - boxSize) * 0.5f;

        GuiTheme theme = graphics.theme();
        int borderColor = hovered ? theme.accentBright() : theme.border();
        int fillColor = checked ? theme.accentBright() : theme.surface();
        int textColor = theme.textPrimary();
        if (disabled) {
            borderColor = dimColor(borderColor, 0.45f);
            fillColor = dimColor(fillColor, 0.45f);
            textColor = theme.textMuted();
        }

        graphics.fillRoundedRectFrame(boxX, boxY, boxSize, boxSize, boxCornerRadius, borderColor, fillColor, borderPx, borderPx, RectCornerRoundMask.ALL);

        if (checked) {
            float iconPad = 1f;
            graphics.drawTexture(checkedTextureId, boxX + iconPad, boxY + iconPad, boxSize - iconPad * 2f, boxSize - iconPad * 2f, disabled ? dimColor(0xFFFFFFFF, 0.6f) : 0xFFFFFFFF);
        }

        String label = labelSupplier.get();
        float labelX = boxX + boxSize + labelGap;
        float labelY = y() + (h() - graphics.getFontCapHeight()) * 0.5f;
        graphics.drawText(label, labelX, labelY, textColor, false, false);
    }
}
