package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.hud;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.common.util.StringUtils;
import cc.fascinated.fascinatedutils.gui.anim.FadeInAnim;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.display.AlphaNode;
import cc.fascinated.fascinatedutils.gui.node.layout.PadNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.ItemStackNode;
import cc.fascinated.fascinatedutils.gui.node.layout.OverlayNode;
import cc.fascinated.fascinatedutils.gui.node.display.ProgressBarNode;
import cc.fascinated.fascinatedutils.gui.node.layout.RowNode;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudComposeContext;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaWidget.CrosshairTarget;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class WawlaHudPanel extends HudPanel {

    private static final int ICON_TEXT_GAP = 5;
    private static final int LINE_GAP = 1;
    private static final int BREAK_BAR_HEIGHT = 2;
    private static final float BREAK_BAR_LERP_SPEED = 14f;

    private static final int TITLE_COLOR = UiColor.argb("#f2f6ff");
    private static final int SOURCE_COLOR = UiColor.argb("#4a5cd3");
    private static final int SUBTITLE_COLOR = UiColor.argb("#aaaaaa");
    private static final int BREAK_BAR_BACKGROUND = UiColor.argb("#44232a33");
    private static final int BREAK_BAR_FILL = UiColor.argb("#ffffffff");
    private static final String TITLE_OPEN = "<color:" + Colors.rgbHex(TITLE_COLOR) + ">";
    private static final String SUBTITLE_OPEN = "<color:" + Colors.rgbHex(SUBTITLE_COLOR) + ">";
    private static final String SOURCE_OPEN = "<i><color:" + Colors.rgbHex(SOURCE_COLOR) + ">";

    private final WawlaWidget wawlaWidget;
    private final FadeInAnim fadeAnim = new FadeInAnim(100f);
    private final FadeInAnim breakBarAnim = new FadeInAnim(150f);
    @Nullable
    private CrosshairTarget lastCrosshairTarget;
    private float smoothedBreakProgress;

    public WawlaHudPanel(WawlaWidget wawlaWidget) {
        super(wawlaWidget, "wawla", 0f);
        this.wawlaWidget = wawlaWidget;
    }

    @Override
    protected @Nullable UiNode compose(HudComposeContext context) {
        float deltaSeconds = context.deltaSeconds();
        CrosshairTarget displayTarget = wawlaWidget.resolveCrosshairTarget(context.editorMode());

        fadeAnim.tick(deltaSeconds);
        breakBarAnim.tick(deltaSeconds);
        if (displayTarget != null) {
            lastCrosshairTarget = displayTarget;
            fadeAnim.show();
        }
        else {
            fadeAnim.hide();
        }
        if (!fadeAnim.isVisible()) {
            return null;
        }

        CrosshairTarget target = lastCrosshairTarget;
        if (target == null) {
            return null;
        }

        if (target.showBreakBar()) {
            float targetBreakProgress = Mth.clamp(target.breakProgress(), 0f, 1f);
            float lerpFactor = targetBreakProgress >= smoothedBreakProgress ? Mth.clamp(deltaSeconds * 20f, 0f, 1f) : Mth.clamp(deltaSeconds * BREAK_BAR_LERP_SPEED, 0f, 1f);
            smoothedBreakProgress = Mth.lerp(lerpFactor, smoothedBreakProgress, targetBreakProgress);
            breakBarAnim.show();
        }
        else {
            breakBarAnim.hide();
            if (!breakBarAnim.isVisible()) {
                smoothedBreakProgress = 0f;
            }
        }

        // The break bar runs along the panel's bottom edge, so the panel's own padding is applied inside the overlay.
        context.padded(false);
        context.opacity(() -> fadeAnim.progress().value());
        OverlayNode overlay = new OverlayNode();
        overlay.addChild(new PadNode().padding(context.padding()).addChild(targetRow(context, target)));
        if (breakBarAnim.isVisible()) {
            float breakProgress = smoothedBreakProgress;
            overlay.addChild(new AlphaNode(() -> breakBarAnim.progress().value())
                    .fullWidth()
                    .height(BREAK_BAR_HEIGHT)
                    .alignY(1f)
                    .addChild(new ProgressBarNode(() -> breakProgress)
                            .setTrackArgb(BREAK_BAR_BACKGROUND)
                            .setFillArgb(BREAK_BAR_FILL)
                            .full()));
        }
        return overlay;
    }

    /**
     * The icon beside the title, subtitle and source lines, centered on each other. Names come from the server
     * (entity names, custom block names), so they are escaped and cannot inject MiniMessage tags.
     */
    private RowNode targetRow(HudComposeContext context, CrosshairTarget target) {
        String rawDisplayName = target.displayName();
        String strippedDisplayName = rawDisplayName == null ? "" : StringUtils.stripColors(rawDisplayName).trim();
        String titleName = strippedDisplayName.isEmpty() ? Objects.toString(target.entityName(), "") : rawDisplayName;
        boolean rightAligned = context.contentAlignment() == 1f;
        float lineAlignment = rightAligned ? 1f : 0f;

        ColumnNode textColumn = new ColumnNode(LINE_GAP).alignY(0.5f);
        textColumn.addChild(context.text(TITLE_OPEN + escapeMiniMessage(titleName) + "</color>").alignX(lineAlignment));
        for (String subtitleLine : target.subtitleLines()) {
            textColumn.addChild(context.text(SUBTITLE_OPEN + subtitleLine + "</color>").alignX(lineAlignment));
        }
        textColumn.addChild(context.text(SOURCE_OPEN + escapeMiniMessage(Objects.toString(target.sourceName(), "")) + "</color></i>").alignX(lineAlignment));

        ItemStackNode icon = new ItemStackNode(target.iconStack()).alignY(0.5f);
        RowNode row = new RowNode(0);
        if (rightAligned) {
            row.addChild(textColumn);
            row.addChild(icon.left(ICON_TEXT_GAP));
        }
        else {
            row.addChild(icon);
            row.addChild(textColumn.left(ICON_TEXT_GAP));
        }
        return row;
    }
}
