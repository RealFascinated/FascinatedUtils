package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextLayoutMetrics;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.Icons;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.widgets.FRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.util.Mth;

public class FModSettingsDetailHeaderCardWidget extends FWidget {
    private static final float CARD_PAD_X_DESIGN = 7f;
    private static final float CARD_PAD_Y_DESIGN = 3f;
    private static final float BACK_SIZE_DESIGN = 14f;

    private final Runnable onBack;
    private final String titleText;
    private final float rowWidthPixels;
    private final boolean showBackButton;
    private boolean hoverBack;

    private FModSettingsDetailHeaderCardWidget(Runnable onBack, String titleText, float rowWidthPixels, boolean showBackButton) {
        this.onBack = onBack;
        this.titleText = titleText == null ? "" : titleText;
        this.rowWidthPixels = Math.max(0f, rowWidthPixels);
        this.showBackButton = showBackButton;
    }

    public static FWidget centeredInContentRow(float settingsContentWidth, float settingsInnerWidth, Runnable onBack, String titleText) {
        float inner = Math.max(0f, settingsInnerWidth);
        float content = Math.max(0f, settingsContentWidth);
        float slack = Math.max(0f, (content - inner) * 0.5f);
        FRowWidget row = new FRowWidget(0f, Align.CENTER);
        row.addChild(new FSpacerWidget(slack, 0f));
        row.addChild(new FModSettingsDetailHeaderCardWidget(onBack, titleText, inner, true));
        row.addChild(new FSpacerWidget(slack, 0f));
        return row;
    }

    /**
     * Same centered shell row as {@link #centeredInContentRow}, but without a back control (title card only).
     *
     * @param settingsContentWidth full row width in shell layout units
     * @param settingsInnerWidth   inner width aligned with settings detail content
     * @param titleText            title shown on the card
     * @return a row widget centering the title card between horizontal slack spacers
     */
    public static FWidget centeredTitleOnlyInContentRow(float settingsContentWidth, float settingsInnerWidth, String titleText) {
        float inner = Math.max(0f, settingsInnerWidth);
        float content = Math.max(0f, settingsContentWidth);
        float slack = Math.max(0f, (content - inner) * 0.5f);
        FRowWidget row = new FRowWidget(0f, Align.CENTER);
        row.addChild(new FSpacerWidget(slack, 0f));
        row.addChild(new FModSettingsDetailHeaderCardWidget(() -> {
        }, titleText, inner, false));
        row.addChild(new FSpacerWidget(slack, 0f));
        return row;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return rowWidthPixels;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        float padY = CARD_PAD_Y_DESIGN;
        float backSize = BACK_SIZE_DESIGN;
        float lineHeight = measure != null ? TextLayoutMetrics.layoutLineHeightPx(measure) : Math.max(1f, ModSettingsTheme.shellDesignBodyLineHeight());
        return 2f * padY + Math.max(lineHeight, backSize);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public boolean wantsPointer() {
        return showBackButton;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        if (!showBackButton) {
            return UiPointerCursor.DEFAULT;
        }
        return hitBack(pointerX, pointerY) ? UiPointerCursor.HAND : UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        hoverBack = showBackButton && hitBack(pointerX, pointerY);
        return false;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hoverBack = false;
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button == 0 && hitBack(pointerX, pointerY)) {
            onBack.run();
            return true;
        }
        return false;
    }

    private boolean hitBack(float pointerX, float pointerY) {
        if (!showBackButton) {
            return false;
        }
        float[] bounds = backButtonBounds();
        return pointerX >= bounds[0] && pointerY >= bounds[1] && pointerX < bounds[0] + bounds[2] && pointerY < bounds[1] + bounds[3];
    }

    private float[] backButtonBounds() {
        float padX = CARD_PAD_X_DESIGN;
        float backSize = BACK_SIZE_DESIGN;
        float backX = x() + padX;
        float backY = y() + (h() - backSize) * 0.5f;
        return new float[]{backX, backY, backSize, backSize};
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        float corner = graphics.theme().cardCornerRadius();
        float borderThickness = UITheme.BORDER_THICKNESS_PX;
        graphics.fillRoundedRectFrame(x(), y(), w(), h(), corner, graphics.theme().border(), graphics.theme().surface(), borderThickness, borderThickness, RectCornerRoundMask.ALL);
        float lineHeight = TextLayoutMetrics.layoutLineHeightPx(graphics);
        float textWidth = graphics.measureTextWidth(titleText, false);
        float textX = x() + (w() - textWidth) * 0.5f;
        float textY = y() + (h() - lineHeight) * 0.5f;
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(graphics.theme().textPrimary()) + ">" + titleText + "</color>", textX, textY, false);
        if (showBackButton) {
            float[] back = backButtonBounds();
            int backIconTint = hoverBack ? graphics.theme().textPrimary() : graphics.theme().textMuted();
            Icons.paintModSettingsBackIcon(graphics, Mth.floor(back[0]), Mth.floor(back[1]), back[2], back[3], backIconTint);
        }
    }
}
