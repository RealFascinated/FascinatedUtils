package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.Colors;
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
import org.jspecify.annotations.Nullable;

public class FModSettingsDetailHeaderCardWidget extends FWidget {
    private static final float CARD_PAD_X_DESIGN = 7f;
    private static final float CARD_PAD_Y_DESIGN = 3f;
    private static final float BACK_SIZE_DESIGN = 14f;

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

    @Nullable
    private final FWidget searchWidget;

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
    private final Runnable onBack;
    private final String titleText;
    private final float rowWidthPixels;
    private final boolean showBackButton;
    @Nullable
    private final Runnable onResetAll;
    private boolean hoverReset;
    private boolean hoverBack;
    private float resetButtonX;
    private float resetButtonY;
    private float resetButtonSize;
    private FModSettingsDetailHeaderCardWidget(Runnable onBack, String titleText, float rowWidthPixels, boolean showBackButton) {
        this(onBack, titleText, rowWidthPixels, showBackButton, null, null);
    }

    private FModSettingsDetailHeaderCardWidget(Runnable onBack, String titleText, float rowWidthPixels, boolean showBackButton, @Nullable FWidget searchWidget, @Nullable Runnable onResetAll) {
        this.onBack = onBack;
        this.titleText = titleText == null ? "" : titleText;
        this.rowWidthPixels = Math.max(0f, rowWidthPixels);
        this.showBackButton = showBackButton;
        this.searchWidget = searchWidget;
        this.onResetAll = onResetAll;
        if (searchWidget != null) {
            addChild(searchWidget);
        }
    }

    /**
     * Same centered shell row as {@link #centeredInContentRow}, but without a back control (title card only).
     *
     * @param settingsContentWidth full row width in shell layout units
     * @param settingsInnerWidth   inner width aligned with settings detail content
     * @param titleText            title shown on the card
     * @return a row widget centering the title card between horizontal slack spacers
     */
    public static FWidget centeredWithSearchAndResetInContentRow(float settingsContentWidth, float settingsInnerWidth, Runnable onBack, String titleText, FWidget searchWidget, Runnable onResetAll) {
        float inner = Math.max(0f, settingsInnerWidth);
        float content = Math.max(0f, settingsContentWidth);
        float slack = Math.max(0f, (content - inner) * 0.5f);
        FRowWidget row = new FRowWidget(0f, Align.CENTER);
        row.addChild(new FSpacerWidget(slack, 0f));
        row.addChild(new FModSettingsDetailHeaderCardWidget(onBack, titleText, inner, true, searchWidget, onResetAll));
        row.addChild(new FSpacerWidget(slack, 0f));
        return row;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return rowWidthPixels;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        float lineHeight = measure != null ? TextLayoutMetrics.layoutLineHeightPx(measure) : Math.max(1f, ModSettingsTheme.shellDesignBodyLineHeight());
        return 2f * CARD_PAD_Y_DESIGN + Math.max(lineHeight, BACK_SIZE_DESIGN);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        if (searchWidget != null) {
            float hGap = 4f;
            float titleW = measure != null ? measure.measureTextWidth(titleText, false) : 0f;
            float resetW = 11f;
            float searchMaxW = 90f;
            float availableX = layoutX + CARD_PAD_X_DESIGN + BACK_SIZE_DESIGN + hGap + titleW + hGap;
            float availableRight = layoutX + layoutWidth - CARD_PAD_X_DESIGN - resetW - hGap;
            float searchW = Math.min(searchMaxW, Math.max(0f, availableRight - availableX));
            float searchX = availableRight - searchW;
            float searchH = layoutHeight - 2f * CARD_PAD_Y_DESIGN;
            searchWidget.layout(measure, searchX, layoutY + CARD_PAD_Y_DESIGN, searchW, searchH);
            resetButtonSize = resetW;
            resetButtonX = layoutX + layoutWidth - CARD_PAD_X_DESIGN - resetW;
            resetButtonY = layoutY + (layoutHeight - resetW) * 0.5f;
        }
    }

    @Override
    public boolean wantsPointer() {
        return showBackButton || onResetAll != null;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        if (showBackButton && hitBack(pointerX, pointerY)) {
            return UiPointerCursor.HAND;
        }
        if (onResetAll != null && hitReset(pointerX, pointerY)) {
            return UiPointerCursor.HAND;
        }
        return UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        hoverBack = showBackButton && hitBack(pointerX, pointerY);
        hoverReset = onResetAll != null && hitReset(pointerX, pointerY);
        return false;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        super.mouseLeave(pointerX, pointerY);
        hoverBack = false;
        hoverReset = false;
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button == 0) {
            if (hitBack(pointerX, pointerY)) {
                onBack.run();
                return true;
            }
            if (onResetAll != null && hitReset(pointerX, pointerY)) {
                onResetAll.run();
                return true;
            }
        }
        return false;
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
        if (searchWidget != null) {
            float hGap = 4f;
            float titleX = x() + CARD_PAD_X_DESIGN + BACK_SIZE_DESIGN + hGap;
            graphics.drawMiniMessageText("<color:" + Colors.rgbHex(graphics.theme().textPrimary()) + ">" + titleText + "</color>", titleX, textY, false);
        }
        else {
            graphics.drawMiniMessageText("<color:" + Colors.rgbHex(graphics.theme().textPrimary()) + ">" + titleText + "</color>", textX, textY, false);
        }
        if (showBackButton) {
            float[] back = backButtonBounds();
            int backIconTint = hoverBack ? graphics.theme().textPrimary() : graphics.theme().textMuted();
            Icons.paintModSettingsBackIcon(graphics, Mth.floor(back[0]), Mth.floor(back[1]), back[2], back[3], backIconTint);
        }
        if (onResetAll != null) {
            int resetIconTint = hoverReset ? graphics.theme().textPrimary() : graphics.theme().textMuted();
            Icons.paintSettingResetCharacter(graphics, Mth.floor(resetButtonX), Mth.floor(resetButtonY), resetButtonSize, resetButtonSize, resetIconTint);
        }
    }

    private boolean hitBack(float pointerX, float pointerY) {
        if (!showBackButton) {
            return false;
        }
        float[] bounds = backButtonBounds();
        return pointerX >= bounds[0] && pointerY >= bounds[1] && pointerX < bounds[0] + bounds[2] && pointerY < bounds[1] + bounds[3];
    }

    private boolean hitReset(float pointerX, float pointerY) {
        return pointerX >= resetButtonX && pointerY >= resetButtonY && pointerX < resetButtonX + resetButtonSize && pointerY < resetButtonY + resetButtonSize;
    }

    private float[] backButtonBounds() {
        float backX = x() + CARD_PAD_X_DESIGN;
        float backY = y() + (h() - BACK_SIZE_DESIGN) * 0.5f;
        return new float[]{backX, backY, BACK_SIZE_DESIGN, BACK_SIZE_DESIGN};
    }
}
