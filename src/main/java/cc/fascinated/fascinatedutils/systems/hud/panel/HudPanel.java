package cc.fascinated.fascinatedutils.systems.hud.panel;

import cc.fascinated.fascinatedutils.systems.hud.ModuleHudState;

import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;

import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.node.HudLayerNode;
import cc.fascinated.fascinatedutils.systems.hud.node.HudPanelNode;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A rectangle of HUD content owned by a {@link HudHostModule}. Every host registers exactly one panel.
 */
public abstract class HudPanel {
    /**
     * Smallest scale accepted from the saved layout and from the HUD editor.
     */
    public static final float MIN_SCALE = 0.25f;
    /**
     * Largest scale accepted from the saved layout and from the HUD editor.
     */
    public static final float MAX_SCALE = 4.0f;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final float DEFAULT_MIN_HEIGHT = 16f;
    private static final String WIDGET_SECTION_PREFIX = "hud.widget.";
    private static final String WIDGET_PROFILER_PREFIX = "widget-";

    private final HudHostModule host;
    @Getter
    private final String id;
    private final float minWidth;
    @Getter
    private final ModuleHudState hudState = new ModuleHudState();
    private final String profilerName;
    private final PerfProbe.Section perfSection;
    private final HudComposeContext composeContext = new HudComposeContext(this);
    private Map<String, String> translations = new HashMap<>();
    private String translationsLanguage = "";

    protected HudPanel(HudHostModule hostModule, String panelId, float minWidth) {
        this.host = hostModule;
        this.id = panelId;
        this.minWidth = minWidth;
        this.profilerName = WIDGET_PROFILER_PREFIX + panelId;
        this.perfSection = PerfProbe.INSTANCE.section(WIDGET_SECTION_PREFIX + panelId);
        applyHudDefaults(hostModule.getHudDefaults());
    }

    /**
     * The HUD-hosting module owning settings/profile state for this panel.
     *
     * @return the host module
     */
    public HudHostModule hudHostModule() {
        return host;
    }

    /**
     * Navigation target opened from the HUD editor's settings button.
     *
     * @return the module whose settings to open, the host module
     */
    public Module hudSettingsNavigationTarget() {
        return host;
    }

    public String getName() {
        return host.getDisplayName();
    }

    public float getMinWidth() {
        return effectiveMinWidth();
    }

    protected float effectiveMinWidth() {
        return minWidth;
    }

    /**
     * Whether this panel is drawn in-world and offered in the HUD editor.
     *
     * @return {@code true} while the host module is enabled and {@link #isPanelVisible()} holds
     */
    public final boolean shouldRenderHudPanel() {
        return host.isEnabled() && isPanelVisible();
    }

    /**
     * Extra visibility condition on top of the host being enabled; hosts that must keep running with their panel
     * hidden (for example one that also does background work) override this together with {@link #hideFromHud()}.
     *
     * @return {@code true} to draw the panel
     */
    protected boolean isPanelVisible() {
        return true;
    }

    /**
     * Removes this panel from the HUD, as the HUD editor's disable button does. Every host has exactly one panel,
     * so the default switches the whole host module off.
     */
    public void hideFromHud() {
        host.setEnabled(false);
    }

    /**
     * Composes this panel's content and wraps it in the chrome node that draws the background and border, applies
     * padding, minimum width and scale, and anchors the panel. Called by the HUD framework once per frame.
     *
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true inside the HUD layout editor preview, where an empty panel still gets a node
     * @param followAnchor whether the panel position is recomputed from its anchor while laid out
     * @return the panel node, or {@code null} when the panel has nothing to show outside the editor
     */
    public final @Nullable HudPanelNode composeNode(float deltaSeconds, boolean editorMode, boolean followAnchor) {
        composeContext.begin(deltaSeconds, editorMode);
        UiNode content = compose(composeContext);
        if (content == null && !editorMode) {
            return null;
        }
        int padding = composeContext.padded() ? composeContext.padding() : 0;
        return new HudPanelNode(this, content, padding, composeContext.opacity(), editorMode, followAnchor);
    }

    /**
     * Composes this panel into the HUD layer under its own profiler scope and perf section. A panel with nothing to
     * show is skipped but still keeps its anchored position up to date.
     *
     * @param layer        the layer the panel's node is added to
     * @param deltaSeconds frame delta in seconds
     * @param canvasWidth  width of the HUD canvas
     * @param canvasHeight height of the HUD canvas
     */
    public final void composeInto(HudLayerNode layer, float deltaSeconds, float canvasWidth, float canvasHeight) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push(profilerName);
        long allocationStart = PerfProbe.INSTANCE.beginAllocation();
        long composeStart = System.nanoTime();
        HudPanelNode panelNode = composeNode(deltaSeconds, false, true);
        if (panelNode == null) {
            recordHudContentSkipped();
            applyHudAnchorToPosition(canvasWidth, canvasHeight);
        }
        else {
            layer.addChild(panelNode);
        }
        perfSection.record(System.nanoTime() - composeStart);
        perfSection.endAllocation(allocationStart);
        profiler.pop();
    }

    /**
     * Escapes MiniMessage tags in text that is not ours (server-controlled names, datapack ids) before it is
     * concatenated into MiniMessage markup, so it renders literally and cannot inject formatting.
     *
     * @param untrustedText text to embed in MiniMessage
     * @return the text with tags escaped
     */
    protected static String escapeMiniMessage(String untrustedText) {
        return MINI_MESSAGE.escapeTags(untrustedText);
    }

    /**
     * Resolves a translation for this panel's host, cached until the selected language changes so it can be
     * called every frame.
     *
     * @param name key below {@code alumite.module.<host>.hud.}
     * @return the translated text
     */
    protected final String hudText(String name) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        if (!language.equals(translationsLanguage)) {
            translations = new HashMap<>();
            translationsLanguage = language;
        }
        String cached = translations.get(name);
        if (cached == null) {
            cached = I18n.get(host.translationKey("hud." + name));
            translations.put(name, cached);
        }
        return cached;
    }

    /**
     * Builds the node subtree this panel shows, from scratch every frame; nothing is cached by the panel. The tree
     * is laid out and drawn inside the panel chrome, so it only describes content: it must not draw a background,
     * pad itself or position itself on the canvas. Use the helpers on the context for text and alignment, and the
     * nodes in {@code gui/node} and {@code gui/node/hud} for everything else.
     *
     * @param context frame inputs, host settings and node helpers
     * @return the content node, or {@code null} when the panel should not be drawn
     */
    protected abstract @Nullable UiNode compose(HudComposeContext context);

    /**
     * Optional alignment override for text-line HUD entries; {@code null} keeps centered text within the panel band.
     *
     * @return override alignment, or {@code null} for default
     */
    protected HudAnchorContentAlignment.@Nullable Horizontal hudTextLineHorizontalAlignmentOverride() {
        return null;
    }

    public final HudAnchorContentAlignment.Horizontal hudTextLineHorizontalAlignment() {
        HudAnchorContentAlignment.Horizontal override = hudTextLineHorizontalAlignmentOverride();
        if (override != null) {
            return override;
        }
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    public HudAnchorContentAlignment.Horizontal hudContentHorizontalAlignment() {
        HudAnchorContentAlignment.Horizontal anchorAlignment = HudAnchorContentAlignment.horizontal(hudState.getHudAnchor());
        if (anchorAlignment != HudAnchorContentAlignment.Horizontal.CENTER) {
            return anchorAlignment;
        }
        if (hudState.getHudAnchor() == HUDWidgetAnchor.BOTTOM) {
            return hudState.isBottomAnchorRightAligned() ? HudAnchorContentAlignment.Horizontal.RIGHT : HudAnchorContentAlignment.Horizontal.LEFT;
        }
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    public void resetPanelLayoutDefaults() {
        hudState.setScale(1.0f);
        applyHudDefaults(host.getHudDefaults());
        hudState.setLastLayoutWidth(-1f);
        hudState.setLastLayoutHeight(-1f);
        hudState.setCommittedLayoutHeight(-1f);
        hudState.setCommittedLayoutWidth(-1f);
    }

    public float getScaledWidth() {
        return layoutWidthForAnchoring() * hudState.getScale();
    }

    public float getScaledHeight() {
        return layoutHeightForAnchoring() * hudState.getScale();
    }

    public boolean containsPoint(float pointerX, float pointerY) {
        return pointerX >= hudState.getPositionX() && pointerX <= hudState.getPositionX() + getScaledWidth() && pointerY >= hudState.getPositionY() && pointerY <= hudState.getPositionY() + getScaledHeight();
    }

    public void applyHudAnchorToPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float maxOffsetX = Math.max(0f, canvasWidth - widgetWidth);
        float maxOffsetY = Math.max(0f, canvasHeight - widgetHeight);
        float offsetX = hudState.getAnchorOffsetX();
        float offsetY = hudState.getAnchorOffsetY();
        float halfCanvasWidth = canvasWidth * 0.5f;
        float halfCanvasHeight = canvasHeight * 0.5f;
        float halfWidgetWidth = widgetWidth * 0.5f;
        float halfWidgetHeight = widgetHeight * 0.5f;
        HudAnchorContentAlignment.Horizontal hAlign = hudContentHorizontalAlignment();

        float resolvedX = switch (hudState.getHudAnchor()) {
            case TOP_LEFT, LEFT -> offsetX;
            case TOP_RIGHT, RIGHT -> canvasWidth - widgetWidth - offsetX;
            case BOTTOM_LEFT -> offsetX;
            case BOTTOM_RIGHT -> canvasWidth - widgetWidth - offsetX;
            case TOP, CENTER -> halfCanvasWidth - halfWidgetWidth + offsetX;
            case BOTTOM -> switch (hAlign) {
                case LEFT -> halfCanvasWidth + offsetX;
                case RIGHT -> halfCanvasWidth - widgetWidth + offsetX;
                case CENTER -> halfCanvasWidth - halfWidgetWidth + offsetX;
            };
        };

        float resolvedY = switch (hudState.getHudAnchor()) {
            case TOP_LEFT, TOP_RIGHT, TOP -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM -> canvasHeight - widgetHeight - offsetY;
            case LEFT, RIGHT -> halfCanvasHeight - halfWidgetHeight + offsetY;
            case CENTER -> halfCanvasHeight - halfWidgetHeight + offsetY;
        };

        hudState.setPositionX(Mth.clamp(resolvedX, 0f, maxOffsetX));
        hudState.setPositionY(Mth.clamp(resolvedY, 0f, maxOffsetY));
    }

    public void captureNearestHudAnchorFromPosition(float canvasWidth, float canvasHeight) {
        hudState.setHudAnchor(nearestAnchor(canvasWidth, canvasHeight));
        syncHudAnchorOffsetsFromCurrentPosition(canvasWidth, canvasHeight);
    }

    public void syncHudAnchorOffsetsFromCurrentPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float halfCanvasWidth = canvasWidth * 0.5f;
        float halfCanvasHeight = canvasHeight * 0.5f;
        float halfWidgetWidth = widgetWidth * 0.5f;
        float halfWidgetHeight = widgetHeight * 0.5f;
        float posX = hudState.getPositionX();
        float posY = hudState.getPositionY();

        switch (hudState.getHudAnchor()) {
            case TOP_LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(posY);
            }
            case TOP_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(posY);
            }
            case BOTTOM_LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case BOTTOM_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case TOP -> {
                hudState.setAnchorOffsetX(posX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(posY);
            }
            case BOTTOM -> {
                // Pick the side from where the panel actually is, not from the offset about to be rewritten.
                boolean rightAligned = posX + halfWidgetWidth < halfCanvasWidth;
                hudState.setBottomAnchorRightAligned(rightAligned);
                hudState.setAnchorOffsetX(rightAligned ? posX + widgetWidth - halfCanvasWidth : posX - halfCanvasWidth);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
            case RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
            case CENTER -> {
                hudState.setAnchorOffsetX(posX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
        }
    }

    public final void recordHudContentSkipped() {
        if (hudState.getLastLayoutWidth() < 0f || hudState.getLastLayoutHeight() < 0f) {
            commitLayout(Math.max(1f, minWidth), DEFAULT_MIN_HEIGHT);
        }
    }

    /**
     * Records a measured layout size as both the last measured and the committed layout size.
     *
     * @param width  layout width in HUD pixels
     * @param height layout height in HUD pixels
     */
    public final void commitLayout(float width, float height) {
        hudState.setLastLayoutWidth(width);
        hudState.setLastLayoutHeight(height);
        hudState.setCommittedLayoutWidth(width);
        hudState.setCommittedLayoutHeight(height);
    }

    private void applyHudDefaults(HudDefaults defaults) {
        hudState.setHudAnchor(defaults.defaultAnchor());
        hudState.setAnchorOffsetX(defaults.defaultXOffset());
        hudState.setAnchorOffsetY(defaults.defaultYOffset());
        hudState.setBottomAnchorRightAligned(defaults.defaultXOffset() < 0f);
        hudState.setPositionX(defaults.defaultXOffset());
        hudState.setPositionY(defaults.defaultYOffset());
    }

    private float layoutWidthForAnchoring() {
        if (hudState.getCommittedLayoutWidth() >= 0f) {
            return hudState.getCommittedLayoutWidth();
        }
        if (hudState.getLastLayoutWidth() >= 0f) {
            return hudState.getLastLayoutWidth();
        }
        return minWidth;
    }

    private float layoutHeightForAnchoring() {
        if (hudState.getCommittedLayoutHeight() >= 0f) {
            return hudState.getCommittedLayoutHeight();
        }
        if (hudState.getLastLayoutHeight() >= 0f) {
            return hudState.getLastLayoutHeight();
        }
        return DEFAULT_MIN_HEIGHT;
    }

    private HUDWidgetAnchor nearestAnchor(float canvasWidth, float canvasHeight) {
        float centerX = hudState.getPositionX() + getScaledWidth() * 0.5f;
        float centerY = hudState.getPositionY() + getScaledHeight() * 0.5f;
        HUDWidgetAnchor chosen = HUDWidgetAnchor.TOP_LEFT;
        float bestDist = Float.MAX_VALUE;
        for (HUDWidgetAnchor candidate : HUDWidgetAnchor.values()) {
            float deltaX = centerX - candidate.referenceX(canvasWidth);
            float deltaY = centerY - candidate.referenceY(canvasHeight);
            float dist = deltaX * deltaX + deltaY * deltaY;
            if (dist < bestDist) {
                bestDist = dist;
                chosen = candidate;
            }
        }
        return chosen;
    }
}
